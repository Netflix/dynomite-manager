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
package com.netflix.dynomitemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask;
import com.netflix.dynomitemanager.monitoring.ServoMetricsTask;
import com.netflix.dynomitemanager.sidecore.aws.UpdateSecuritySettings;
import com.netflix.dynomitemanager.sidecore.backup.SnapshotTask;
import com.netflix.dynomitemanager.sidecore.backup.RestoreTask;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskScheduler;
import com.netflix.dynomitemanager.sidecore.utils.ProcessMonitorTask;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import com.netflix.dynomitemanager.sidecore.utils.ProxyAndStorageResetTask;
import com.netflix.dynomitemanager.dynomite.DynomiteYamlTuneTask;
import com.netflix.dynomitemanager.sidecore.storage.Bootstrap;
import com.netflix.dynomitemanager.sidecore.storage.StorageProcessManager;
import com.netflix.dynomitemanager.sidecore.storage.WarmBootstrapTask;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.Monitors;

/**
 * Start the Dynomite Manager (DM) server, set DM's status and start the
 * following tasks:
 *
 * <ul>
 * <li>{@link com.netflix.dynomitemanager.sidecore.aws.UpdateSecuritySettings}:
 * In a multi-DC deployment, update the AWS security group (SG) inbound traffic
 * filters.
 * <li>{@link DynomiteYamlTuneTask}: Write the
 * dynomite.yaml configuration file.
 * <li>{@link com.netflix.dynomitemanager.sidecore.backup.RestoreTask}: If
 * restore mode, then restore data from an object store (i.e. S3).
 * <li>{@link com.netflix.dynomitemanager.sidecore.storage.WarmBootstrapTask}:
 * If warm bootstrap mode, then warm the storage backend by syncing data from a
 * peer.
 * <li>{@link com.netflix.dynomitemanager.sidecore.utils.ProxyAndStorageResetTask}:
 * If cold bootstrap mode, then stop any in progress sync, reset storage backend
 * to master, and restart dynomite proxy (if necessary).
 * <li>{@link com.netflix.dynomitemanager.sidecore.backup.SnapshotTask}: If
 * backups are enabled, then add the backup snapshot task.
 * <li>{@link com.netflix.dynomitemanager.monitoring.ServoMetricsTask}: Publish
 * metrics via Servo.
 * <li>{@link com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask}:
 * Update metrics obtained via Redis INFO command.
 * <li>{@link com.netflix.dynomitemanager.sidecore.utils.ProcessMonitorTask}:
 * Monitor the dynomite and redis-server processes, and restart as necessary.
 * </ul>
 */
@Singleton
public class FloridaServer {
    private final TaskScheduler scheduler;
    private final IConfiguration config;
    private final InstanceIdentity id;
    private final Sleeper sleeper;
    private final DynomiteYamlTuneTask dynomiteYamlTuneTask;
    private final IDynomiteProcess dynProcess;
    private final StorageProcessManager storageProcess;
    private final InstanceState state;
    private static final Logger logger = LoggerFactory.getLogger(FloridaServer.class);

    static {
        System.setProperty("archaius.configurationSource.defaultFileName", "dynomitemanager.properties");
    }

    @Inject
    public FloridaServer(IConfiguration config, TaskScheduler scheduler, InstanceIdentity id, Sleeper sleeper,
	    DynomiteYamlTuneTask dynomiteYamlTuneTask, InstanceState state, IDynomiteProcess dynProcess, StorageProcessManager storageProcess) {

	this.config = config;
	this.scheduler = scheduler;
	this.id = id;
	this.sleeper = sleeper;
	this.dynomiteYamlTuneTask = dynomiteYamlTuneTask;
	this.state = state;
	this.dynProcess = dynProcess;
	this.storageProcess = storageProcess;


	DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(state));

    }

    /**
     * Start Dynomite Manager.
     *
     * @throws Exception
     */
    public void initialize() throws Exception {
	if (id.getInstance().isOutOfService())
	    return;

	logger.info("Initializing Dynomite Manager now ...");

	state.setSideCarProcessAlive(true);
	state.setBootstrapStatus(Bootstrap.NOT_STARTED);

	if (config.isMultiRegionedCluster()) {
	    scheduler.runTaskNow(UpdateSecuritySettings.class);
	    if (id.isReplace() || id.isTokenPregenerated()) {
		long initTime = 100 + (int) (Math.random() * ((200 - 100) + 1));

		logger.info("Sleeping " + initTime + "seconds -> a node is replaced or token is pregenerated.");
		sleeper.sleep(initTime * 1000);
	    } else if (UpdateSecuritySettings.firstTimeUpdated) {
		logger.info("Sleeping 60 seconds -> first time security settings are updated");
		sleeper.sleep(60 * 1000);
	    }

	    scheduler.addTask(UpdateSecuritySettings.JOBNAME, UpdateSecuritySettings.class,
		    UpdateSecuritySettings.getTimer(id));
	}

	// scheduler.runTaskNow(DynomiteYamlTuneTask.class);
	// Invoking the task directly as any errors in this task
	// should not let Florida continue. However, we don't want to kill
	// the Florida process, but, want it to be stuck.
	logger.info("Running DynomiteYamlTuneTask and updating configuration.");
	dynomiteYamlTuneTask.execute();

	// Determine if we need to restore from backup else start Dynomite.
	if (config.isRestoreEnabled()) {
	    logger.info("Restore is enabled.");
	    scheduler.runTaskNow(RestoreTask.class); // restore from the AWS
	    logger.info("Scheduled task " + RestoreTask.TaskName);
	} else { // no restores needed
	    logger.info("Restore is disabled.");

	    // Bootstrapping only if this is a new node.
	    if (config.isForceWarm() || (config.isWarmBootstrap() && id.isReplace())) {
		if (config.isForceWarm()) {
		    logger.info("Enforcing warm up.");
		}
		logger.info("Warm bootstrapping node. Scheduling BootstrapTask now!");
		dynProcess.stop();
		scheduler.runTaskNow(WarmBootstrapTask.class);
	    } else {
		logger.info("Cold bootstraping, launching storage process.");
		storageProcess.start();
		sleeper.sleepQuietly(1000); // 1s
		logger.info("Launching dynomite process.");
		dynProcess.start();
		sleeper.sleepQuietly(1000); // 1s
		scheduler.runTaskNow(ProxyAndStorageResetTask.class);
	    }
	}

	// Backup
	if (config.isBackupEnabled() && config.getBackupHour() >= 0) {
	    scheduler.addTask(SnapshotTask.TaskName, SnapshotTask.class, SnapshotTask.getTimer(config));
	}

	// Metrics
	scheduler.addTask(ServoMetricsTask.TaskName, ServoMetricsTask.class, ServoMetricsTask.getTimer());
	scheduler.addTask(RedisInfoMetricsTask.TaskName, RedisInfoMetricsTask.class, RedisInfoMetricsTask.getTimer());

	// Routine monitoring and restarting dynomite or storage processes as
	// needed.
	scheduler.addTask(ProcessMonitorTask.JOBNAME, ProcessMonitorTask.class, ProcessMonitorTask.getTimer());

	logger.info("Starting task scheduler");
	scheduler.start();
    }

    public InstanceIdentity getId() {
	return id;
    }

    public TaskScheduler getScheduler() {
	return scheduler;
    }

    public IConfiguration getConfiguration() {
	return config;
    }

}
