/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.backup;

import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.LOCAL_ADDRESS;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.netflix.dynomitemanager.backup.SnapshotBackup;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.ThreadSleeper;
import com.netflix.dynomitemanager.sidecore.scheduler.CronTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.CronTimer.DayOfWeek;

/**
 * Task for taking snapshots to S3
 */
@Singleton
public class SnapshotBackup extends Task
{
	public static final String TaskName =  "SnapshotBackup";
	private static final Logger logger = LoggerFactory.getLogger(SnapshotBackup.class);
    private final ThreadSleeper sleeper = new ThreadSleeper();
    private final ICredential cred;
    private final InstanceIdentity iid;
    private final InstanceState state;
    private final IStorageProxy storageProxy;

    private final int storageRetries = 5;
    
    @Inject
    public SnapshotBackup(IConfiguration config, InstanceIdentity id, ICredential cred, InstanceState state,
    		IStorageProxy storageProxy)
    {
        super(config);
        this.cred = cred;
        this.iid = id;
        this.state = state;
        this.storageProxy = storageProxy;
    }
    
    public void execute() throws Exception
    {
    	
    	if(!state.isRestoring() && !state.isBootstrapping()){
    	   /** Iterate five times until storage (Redis) is ready.
    	    *  We need storage to be ready to dumb the data,
    	    *  otherwise we may backup older data. Another case,
    	    *  is that the thread that starts Dynomite has not 
    	    *  started Redis yet.
    	    */
    	   int i = 0;
    	   for (i=0; i < this.storageRetries; i++){
               if (!this.state.isStorageAlive()) { 
                   //sleep 2 seconds to make sure Dynomite process is up, Storage process is up.
                   sleeper.sleepQuietly(2000);
               }
               else{
            	   this.state.setBackingup(true);
            	   // the storage proxy takes a snapshot or compacts data
            	   boolean snapshot = this.storageProxy.takeSnapshot();
                   File file = null;
                   if(config.isAof()){
                	   file = new File(config.getPersistenceLocation() + "/appendonly.aof");
                   }
                   else {
                	   file = new File(config.getPersistenceLocation() + "/nfredis.rdb");
                   }
                   // upload the data to S3
                   if (file.length() > 0 && snapshot == true) {
                	   uploadToS3(file);
                	   logger.info("S3 Backup Completed!");
                   }
                   else {
                       logger.warn("S3 backup: Redis AOF file length is zero - nothing to backup");
                   }
            	   break;
               }
    	   }
    	
    	   if (i == this.storageRetries) {
        	   logger.error("S3 backup Failed: Redis was not up after " + this.storageRetries + " retries");
    	   }
           this.state.setBackingup(false);
    	}
    	else {
    		logger.error("S3 backup Failed: Restore is happening");
    	}

    }
    
    @Override
    public String getName()
    {
        return TaskName;
    }
    
    /**
     * Returns a timer that enables this task to run on a scheduling basis defined by FP
     * if the BackupSchedule == week, it runs on Monday
     * if the BackupSchedule == day, it runs everyday.
     * @return TaskTimer
     */
    public static TaskTimer getTimer(IConfiguration config)
    {
        int hour = config.getBackupHour();
        if (config.getBackupSchedule().equals("week")){
        	return new CronTimer(DayOfWeek.MON, hour, 1, 0);
        }
        return new CronTimer(hour, 1, 0);
 
    }

    
    /**
     * Uses the Amazon S3 API to upload the AOF/RDB to S3
     * Filename: Backup location + DC + Rack + App + Token
     */
    private void uploadToS3(File file)
    {
    	logger.info("Snapshot backup: sending " + file.length() + " bytes to S3");

    	AmazonS3Client s3Client = new AmazonS3Client(this.cred.getAwsCredentialProvider());
        try {
            // Checking if the S3 bucket exists, and if does not, then we create it
            if(!(s3Client.doesBucketExist(config.getBucketName()))) {
      	       logger.error("Bucket with name: " + config.getBucketName() + " does not exist");
            }
            else {
                logger.info("Uploading data to S3\n");
                DateTime now = DateTime.now();
                DateTime todayStart = now.withTimeAtStartOfDay();
                
                /* Key name is comprised of the 
                * backupDir + DC + Rack + token + Date
                */
                String keyName = 
                		config.getBackupLocation() + "/" +
                		iid.getInstance().getDatacenter() + "/" +
                		iid.getInstance().getRack() + "/" +
                		iid.getInstance().getToken() + "/" +
                		todayStart.getMillis();

                // Get bucket location.
                logger.info("S3 Bucket Name:" + config.getBucketName());
                logger.info("Key in Bucket: " + keyName);
         	    s3Client.putObject(new PutObjectRequest(
        		            config.getBucketName(), keyName, file));
            }
            

   

       } catch (AmazonServiceException ase) {
    	   
    	   logger.error("AmazonServiceException;" +
           		" request made it to Amazon S3, but was rejected with an error ");
    	   logger.error("Error Message:    " + ase.getMessage());
    	   logger.error("HTTP Status Code: " + ase.getStatusCode());
    	   logger.error("AWS Error Code:   " + ase.getErrorCode());
    	   logger.error("Error Type:       " + ase.getErrorType());
    	   logger.error("Request ID:       " + ase.getRequestId());
           
       } catch (AmazonClientException ace) {
    	   logger.error("AmazonClientException;"+
           		" the client encountered " +
                   "an internal error while trying to " +
                   "communicate with S3, ");
    	   logger.error("Error Message: " + ace.getMessage());
       }
    }
   
}

