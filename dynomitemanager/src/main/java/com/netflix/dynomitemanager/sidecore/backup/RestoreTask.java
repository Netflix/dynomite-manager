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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.defaultimpl.StorageProcessManager;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
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
    private final IDynomiteProcess dynProcess;
    private final Sleeper sleeper;
    private final Restore restore;
    private StorageProcessManager storageProcessMgr;

    @Inject
    public RestoreTask(IConfiguration config, InstanceIdentity id, ICredential cred, InstanceState state,
	    IStorageProxy storageProxy, IDynomiteProcess dynProcess, Sleeper sleeper, Restore restore,
	    StorageProcessManager storageProcessMgr) {
	super(config);
	this.cred = cred;
	this.iid = id;
	this.state = state;
	this.storageProxy = storageProxy;
	this.dynProcess = dynProcess;
	this.sleeper = sleeper;
	this.restore = restore;
	this.storageProcessMgr = storageProcessMgr;
    }

    public void execute() throws Exception {
	this.state.setRestoring(true);
	this.state.setFirstRestore(false);
	/**
	 * Set the status of the restore to "false" every time we start a
	 * restore. This will ensure that prior to restore we recapture the
	 * status of the restore.
	 */
	this.state.setRestoreStatus(false);

	// stop dynomite process
	this.dynProcess.stop();

	// stop storage process
	this.storageProcessMgr.stop();

	// restore from Object Storage
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
	    // start storage process without loading data
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
