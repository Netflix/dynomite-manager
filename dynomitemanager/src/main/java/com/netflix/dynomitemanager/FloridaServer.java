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
package com.netflix.dynomitemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.dynomite.DynomiteYamlTask;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask;
import com.netflix.dynomitemanager.monitoring.ServoMetricsTask;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.aws.UpdateSecuritySettings;
import com.netflix.dynomitemanager.sidecore.backup.SnapshotTask;
import com.netflix.dynomitemanager.sidecore.backup.RestoreTask;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskScheduler;
import com.netflix.dynomitemanager.sidecore.utils.ProcessMonitorTask;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import com.netflix.dynomitemanager.sidecore.utils.ProxyAndStorageResetTask;
import com.netflix.dynomitemanager.sidecore.utils.WarmBootstrapTask;
import com.netflix.dynomitemanager.sidecore.storage.Bootstrap;
import com.netflix.dynomitemanager.sidecore.storage.StorageProcessManager;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.Monitors;

/**
 * Start all tasks here - Property update task - Backup task - Restore task -
 * Incremental backup
 */
@Singleton
public class FloridaServer {
    private final TaskScheduler scheduler;
    private final IConfiguration config;
    private final InstanceIdentity id;
    private final Sleeper sleeper;
    private final DynomiteYamlTask tuneTask;
    private final ProcessMonitorTask processMonitorTask;
    private final StorageProcessManager storageProcess;
    private final IFloridaProcess dynProcess;
    private final InstanceState state;
    private static final Logger logger = LoggerFactory.getLogger(FloridaServer.class);

    @Inject
    public FloridaServer(IConfiguration config, TaskScheduler scheduler, InstanceIdentity id, Sleeper sleeper,
            DynomiteYamlTask tuneTask, ProcessMonitorTask processMonitorTask, InstanceState state, IFloridaProcess dynProcess,
            StorageProcessManager storageProcess) {
        this.config = config;
        this.scheduler = scheduler;
        this.id = id;
        this.sleeper = sleeper;
        this.tuneTask = tuneTask;
        this.processMonitorTask = processMonitorTask;
        this.state = state;
        this.dynProcess = dynProcess;
        this.storageProcess = storageProcess;


        DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(state));
    }

    public void initialize() throws Exception {
        if (id.getInstance().isOutOfService())
            return;

        logger.info("Initializing Florida Server now ...");

        state.setSideCarProcessAlive(true);
        state.setBootstrapStatus(Bootstrap.NOT_STARTED);

        if (config.isMultiRegionedCluster()) {
            scheduler.runTaskNow(UpdateSecuritySettings.class);
            // sleep for 130 sec if this is a new node with new IP for SG to be
            // updated by other seed nodes
            if (id.isReplace() || id.isTokenPregenerated()) {
                logger.info("Sleeping 130 seconds -> a node is replaced or token is pregenerated.");
                sleeper.sleep(130 * 1000);
            } else if (UpdateSecuritySettings.firstTimeUpdated) {
                logger.info("Sleeping 60 seconds -> first time security settings are updated");
                sleeper.sleep(60 * 1000);
            }

            scheduler.addTask(UpdateSecuritySettings.JOBNAME, UpdateSecuritySettings.class,
                    UpdateSecuritySettings.getTimer(id));
        }

        // scheduler.runTaskNow(TuneTask.class);
        // Invoking the task directly as any errors in this task
        // should not let Florida continue. However, we don't want to kill
        // the Florida process, but, want it to be stuck.
        logger.info("Running TuneTask and updating configuration.");
        tuneTask.execute();

        // Determine if we need to restore from backup else start Dynomite.
        if (config.isRestoreEnabled()) {
            logger.info("Restore is enabled.");
            scheduler.runTaskNow(RestoreTask.class); // restore from the AWS
            logger.info("Scheduled task " + RestoreTask.TaskName);
        } else { // no restores needed
            logger.info("Restore is disabled.");

            /**
             * Bootstrapping cases 1. The user has enforced warm up through an
             * FP 2. It is a new node that replaces an existing token (node
             * termination) 3. An existing token exists and Storage is not alive
             * (node reboot)
             */
            boolean warmUp = false;
            if (config.isForceWarm()) {
                logger.info("force bootstrap -> warm up");
                warmUp = true;
            } else if (config.isWarmBootstrap() && id.isReplace()) {
                logger.info("Instance replacement -> warm up");
                warmUp = true;
            } else if (config.isWarmBootstrap() && !id.isNewToken() && !state.isStorageAlive()) {
                logger.info("Not a new token and Storage is down -> warm up");
                warmUp = true;
            }

            if (warmUp) {
                logger.info("Warm bootstraping node. Scheduling BootstrapTask now!");
                dynProcess.stop();
                scheduler.runTaskNow(WarmBootstrapTask.class);
            } else {
                logger.info("Cold bootstraping, launching storage process.");
                storageProcess.start();
                sleeper.sleepQuietly(2000); // 2s
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
