/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration;
import com.netflix.dynomitemanager.defaultimpl.StorageProcessManager;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.JedisUtils;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;


/**
 * Task for restoring snapshots from object storage
 */

@Singleton
public class RestoreTask extends Task {
	public static final String TaskName = "RestoreTask";
	private static final Logger logger = LoggerFactory.getLogger(RestoreTask.class);
	private final ICredential cred;
	private final InstanceIdentity iid;
	private final InstanceState state;
	private final IStorageProxy storageProxy;
	private final IFloridaProcess dynProcess;
	private final Sleeper sleeper;
	private final Restore restore;


	@Inject
	private StorageProcessManager storageProcessMgr;

	@Inject
	public RestoreTask(IConfiguration config, InstanceIdentity id, ICredential cred, InstanceState state,
					   IStorageProxy storageProxy, IFloridaProcess dynProcess, Sleeper sleeper, Restore restore) {
		super(config);
		this.cred = cred;
		this.iid = id;
		this.state = state;
		this.storageProxy = storageProxy;
		this.dynProcess = dynProcess;
		this.sleeper = sleeper;
		this.restore = restore;
	}


	public void execute() throws Exception {
		this.state.setRestoring(true);
		this.state.setFirstRestore(false);
		/**
		 * Set the status of the restore to "false" every time we start a restore.
		 * This will ensure that prior to restore we recapture the status of the restore.
		 */
		this.state.setRestoreStatus(false);

    	/* stop dynomite process */
		this.dynProcess.stop();

    	/* stop storage process */
		this.storageProcessMgr.stop();

    	/* restore from Object Storage */
		if (restore.restoreData(config.getRestoreDate())) {
        	/* start storage process and load data */
			logger.info("Restored successful: Starting storage process with loading data.");
			this.storageProcessMgr.start();
			if (!this.storageProxy.loadingData()) {
				logger.error("Restore not successful: Restore failed because of Redis.");
			}
			logger.info("Restore Completed, sleeping 5 seconds before starting Dynomite!");

			sleeper.sleepQuietly(5000);
			this.dynProcess.start();
			logger.info("Dynomite started");
			this.state.setRestoreStatus(true);
		} else {
        	/* start storage process without loading data */
			logger.error("Restore not successful: Starting storage process without loading data.");
		}
		this.state.setRestoring(false);
		this.state.setRestoreTime(DateTime.now());
	}

	@Override
	public String getName() {
		return TaskName;
	}

}
