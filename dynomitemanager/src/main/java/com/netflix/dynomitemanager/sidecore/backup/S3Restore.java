/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.backup;

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
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

@Singleton
public class S3Restore implements Restore {

	private static final Logger logger = LoggerFactory.getLogger(S3Restore.class);

	@Inject private IConfiguration config;

	@Inject private ICredential cred;

	@Inject private InstanceIdentity iid;

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
				String keyName = config.getBackupLocation() + "/" +
						iid.getInstance().getDatacenter() + "/" +
						iid.getInstance().getRack() + "/" +
						iid.getInstance().getToken() + "/" +
						time;

				logger.info("S3 Bucket Name: " + config.getBucketName());
				logger.info("Key in Bucket: " + keyName);

				// Checking if the S3 bucket exists, and if does not, then we create it
				if (!(s3Client.doesBucketExist(config.getBucketName()))) {
					logger.error("Bucket with name: " + config.getBucketName() + " does not exist");
				} else {
					S3Object s3object = s3Client
							.getObject(new GetObjectRequest(config.getBucketName(),
									keyName));

					logger.info("Content-Type: " + s3object.getObjectMetadata().getContentType());

					String filepath = null;

					if (config.isAof()) {
						filepath = config.getPersistenceLocation() + "/appendonly.aof";
					} else {
						filepath = config.getPersistenceLocation() + "/nfredis.rdb";
					}

					IOUtils.copy(s3object.getObjectContent(),
							new FileOutputStream(new File(filepath)));
				}
				return true;
			} catch (AmazonServiceException ase) {

				logger.error("AmazonServiceException;"
						+ " request made it to Amazon S3, but was rejected with an error ");
				logger.error("Error Message:    " + ase.getMessage());
				logger.error("HTTP Status Code: " + ase.getStatusCode());
				logger.error("AWS Error Code:   " + ase.getErrorCode());
				logger.error("Error Type:       " + ase.getErrorType());
				logger.error("Request ID:       " + ase.getRequestId());

			} catch (AmazonClientException ace) {
				logger.error("AmazonClientException;" +
						" the client encountered " +
						"an internal error while trying to " +
						"communicate with S3, ");
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
