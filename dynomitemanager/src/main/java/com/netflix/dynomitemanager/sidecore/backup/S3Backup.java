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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.InstanceIdentity;

@Singleton
public class S3Backup implements Backup {

	private static final Logger logger = LoggerFactory.getLogger(S3Backup.class);
	private final long initPartSize =
			500 * 1024 * 1024; // we set the part size equal to 500MB. We do not want this too large
	// and run out of heap space

	@Inject private IConfiguration config;

	@Inject private ICredential cred;

	@Inject private InstanceIdentity iid;

	/**
	 * Uses the Amazon S3 API to upload the AOF/RDB to S3
	 * Filename: Backup location + DC + Rack + App + Token
	 */
	@Override
	public boolean upload(File file, DateTime todayStart) {
		logger.info("Snapshot backup: sending " + file.length() + " bytes to S3");

        	// Key name is comprised of the backupDir + DC + Rack + token + Date
		String keyName = config.getBackupLocation() + "/" +
				iid.getInstance().getDatacenter() + "/" +
				iid.getInstance().getRack() + "/" +
				iid.getInstance().getToken() + "/" +
				todayStart.getMillis();

		// Get bucket location.
		logger.info("Key in Bucket: " + keyName);
		logger.info("S3 Bucket Name:" + config.getBucketName());

		AmazonS3Client s3Client = new AmazonS3Client(cred.getAwsCredentialProvider());

		try {
			// Checking if the S3 bucket exists, and if does not, then we create it
			if (!(s3Client.doesBucketExist(config.getBucketName()))) {
				logger.error("Bucket with name: " + config.getBucketName() + " does not exist");
				return false;
			} else {
				logger.info("Uploading data to S3\n");
				// Create a list of UploadPartResponse objects. You get one of these for
				// each part upload.
				List<PartETag> partETags = new ArrayList<PartETag>();

				InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
						config.getBucketName(), keyName);

				InitiateMultipartUploadResult initResponse = s3Client
						.initiateMultipartUpload(initRequest);

				long contentLength = file.length();
				long filePosition = 0;
				long partSize = this.initPartSize;

				try {
					for (int i = 1; filePosition < contentLength; i++) {
						// Last part can be less than initPartSize (500MB). Adjust part size.
						partSize = Math.min(partSize, (contentLength - filePosition));

						// Create request to upload a part.
						UploadPartRequest uploadRequest = new UploadPartRequest()
								.withBucketName(config.getBucketName()).withKey(keyName)
								.withUploadId(initResponse.getUploadId())
								.withPartNumber(i).withFileOffset(filePosition)
								.withFile(file).withPartSize(partSize);

						// Upload part and add response to our list.
						partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());

						filePosition += partSize;
					}

					CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
							config.getBucketName(), keyName, initResponse.getUploadId(),
							partETags);

					s3Client.completeMultipartUpload(compRequest);

				} catch (Exception e) {
					logger.error("Abosting multipart upload due to error");
					s3Client.abortMultipartUpload(
							new AbortMultipartUploadRequest(config.getBucketName(), keyName,
									initResponse.getUploadId()));
				}

				return true;
			}
		} catch (AmazonServiceException ase) {

			logger.error("AmazonServiceException;"
					+ " request made it to Amazon S3, but was rejected with an error ");
			logger.error("Error Message:    " + ase.getMessage());
			logger.error("HTTP Status Code: " + ase.getStatusCode());
			logger.error("AWS Error Code:   " + ase.getErrorCode());
			logger.error("Error Type:       " + ase.getErrorType());
			logger.error("Request ID:       " + ase.getRequestId());
			return false;

		} catch (AmazonClientException ace) {
			logger.error("AmazonClientException;" +
					" the client encountered " +
					"an internal error while trying to " +
					"communicate with S3, ");
			logger.error("Error Message: " + ace.getMessage());
			return false;
		}
	}
}
