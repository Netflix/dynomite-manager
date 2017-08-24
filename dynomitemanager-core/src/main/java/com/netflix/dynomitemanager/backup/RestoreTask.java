package com.netflix.dynomitemanager.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.config.InstanceState;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.storage.StorageProcessManager;
import com.netflix.dynomitemanager.storage.StorageProxy;
import com.netflix.nfsidecar.backup.Restore;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.utils.Sleeper;

/**
 * Task for restoring snapshots from object storage
 */

@Singleton
public class RestoreTask extends Task {
    public static final String TaskName = "RestoreTask";
    private static final Logger logger = LoggerFactory.getLogger(RestoreTask.class);
    private final InstanceState state;
    private final StorageProxy storageProxy;
    private final IDynomiteProcess dynProcess;
    private final Sleeper sleeper;
    private final Restore restore;
    private StorageProcessManager storageProcessMgr;
    private final FloridaConfig config;

    @Inject
    public RestoreTask(FloridaConfig config, InstanceState state, StorageProxy storageProxy,
            IDynomiteProcess dynProcess, Sleeper sleeper, Restore restore, StorageProcessManager storageProcessMgr) {
        this.config = config;
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
         * Set the status of the restore to "false" every time we start a
         * restore. This will ensure that prior to restore we recapture the
         * status of the restore.
         */
        this.state.setRestoreStatus(false);

        /* stop dynomite process */
        this.dynProcess.stop();

        // stop storage process
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
