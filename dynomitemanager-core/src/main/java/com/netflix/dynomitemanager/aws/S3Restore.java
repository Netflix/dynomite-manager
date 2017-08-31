package com.netflix.dynomitemanager.aws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.nfsidecar.aws.ICredential;
import com.netflix.nfsidecar.backup.Restore;
import com.netflix.nfsidecar.config.AWSCommonConfig;
import com.netflix.nfsidecar.identity.InstanceIdentity;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

@Singleton
public class S3Restore implements Restore {

    private static final Logger logger = LoggerFactory.getLogger(S3Restore.class);

    @Inject
    private AWSCommonConfig commonConfig;

    @Inject 
    private FloridaConfig floridaConfig;
    
    @Inject
    private ICredential cred;

    @Inject
    private InstanceIdentity iid;

    /**
     * Uses the Amazon S3 API to restore from S3
     */
    @Override
    public boolean restoreData(String dateString) {
        long time = restoreTime(dateString);
        if (time > -1) {
            logger.info("Restoring data from S3.");
            AmazonS3Client s3Client = new AmazonS3Client(cred.getAwsCredentialProvider());

            try {
                /* construct the key for the backup data */
                String keyName = commonConfig.getBackupLocation() + "/" + iid.getInstance().getDatacenter() + "/"
                        + iid.getInstance().getRack() + "/" + iid.getInstance().getToken() + "/" + time;

                logger.info("S3 Bucket Name: " + commonConfig.getBucketName());
                logger.info("Key in Bucket: " + keyName);

                // Checking if the S3 bucket exists, and if does not, then we
                // create it
                if (!(s3Client.doesBucketExist(commonConfig.getBucketName()))) {
                    logger.error("Bucket with name: " + commonConfig.getBucketName() + " does not exist");
                } else {
                    S3Object s3object = s3Client.getObject(new GetObjectRequest(commonConfig.getBucketName(), keyName));

                    logger.info("Content-Type: " + s3object.getObjectMetadata().getContentType());

                    String filepath = null;

                    if (floridaConfig.persistenceType().equals("aof")) {
                        filepath = floridaConfig.getPersistenceLocation() + "/appendonly.aof";
                    } else {
                        filepath = floridaConfig.getPersistenceLocation() + "/nfredis.rdb";
                    }

                    IOUtils.copy(s3object.getObjectContent(), new FileOutputStream(new File(filepath)));
                }
                return true;
            } catch (AmazonServiceException ase) {

                logger.error(
                        "AmazonServiceException;" + " request made it to Amazon S3, but was rejected with an error ");
                logger.error("Error Message:    " + ase.getMessage());
                logger.error("HTTP Status Code: " + ase.getStatusCode());
                logger.error("AWS Error Code:   " + ase.getErrorCode());
                logger.error("Error Type:       " + ase.getErrorType());
                logger.error("Request ID:       " + ase.getRequestId());

            } catch (AmazonClientException ace) {
                logger.error("AmazonClientException;" + " the client encountered "
                        + "an internal error while trying to " + "communicate with S3, ");
                logger.error("Error Message: " + ace.getMessage());
            } catch (IOException io) {
                logger.error("File storing error: " + io.getMessage());
            }
        } else {
            logger.error("Date in FP: " + dateString);
        }
        return false;
    }

    private long restoreTime(String dateString) {
        logger.info("Date to restore to: " + dateString);

        DateTimeFormatter formatter = null;
        try {
            formatter = DateTimeFormat.forPattern("yyyyMMdd");
        } catch (Exception e) {
            logger.error("Restore fast property not formatted properly " + e.getMessage());
            return -1;
        }

        DateTime dt = formatter.parseDateTime(dateString);
        DateTime dateBackup = dt.withTimeAtStartOfDay();
        return dateBackup.getMillis();

    }

}
