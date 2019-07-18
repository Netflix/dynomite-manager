/**
 * Copyright 2013 Netflix, Inc.
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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.dynomitemanager.backup.RestoreTask;
import com.netflix.dynomitemanager.backup.SnapshotTask;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.config.InstanceState;
import com.netflix.dynomitemanager.dynomite.DynomiteProcessManager;
import com.netflix.dynomitemanager.dynomite.DynomiteRest;
import com.netflix.dynomitemanager.dynomite.DynomiteYamlTask;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.dynomite.ProxyAndStorageResetTask;
import com.netflix.dynomitemanager.monitoring.ProcessMonitorTask;
import com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask;
import com.netflix.dynomitemanager.monitoring.ServoMetricsTask;
import com.netflix.dynomitemanager.storage.*;
import com.netflix.nfsidecar.aws.UpdateSecuritySettings;
import com.netflix.nfsidecar.config.CommonConfig;
import com.netflix.nfsidecar.identity.InstanceIdentity;
import com.netflix.nfsidecar.scheduler.TaskScheduler;
import com.netflix.nfsidecar.utils.Sleeper;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.Monitors;

/**
 * Start all tasks here - Property update task - Backup task - Restore task -
 * Incremental backup
 */
@Singleton
public class FloridaServer {
    private final TaskScheduler scheduler;
    private final FloridaConfig floridaConfig;
    private final CommonConfig commonConfig;
    private final InstanceIdentity id;
    private final Sleeper sleeper;
    private final DynomiteYamlTask tuneTask;
    private final IDynomiteProcess dynProcess;
    private final InstanceState state;
    private final StorageProcessManager storageProcess;
    private final StorageProxy storageProxy;
    private static final Logger logger = LoggerFactory.getLogger(FloridaServer.class);

    private final DynamicStringProperty readConsistencyFP;
    private final DynamicStringProperty writeConsistencyFP;

    @Inject
    public FloridaServer(FloridaConfig floridaConfig, CommonConfig commonConfig, TaskScheduler scheduler,
            InstanceIdentity id, Sleeper sleeper, DynomiteYamlTask tuneTask, InstanceState state,
            IDynomiteProcess dynProcess, StorageProcessManager storageProcess, StorageProxy storageProxy) {
        this.floridaConfig = floridaConfig;
        this.commonConfig = commonConfig;
        this.scheduler = scheduler;
        this.id = id;
        this.sleeper = sleeper;
        this.tuneTask = tuneTask;
        this.state = state;
        this.dynProcess = dynProcess;
        this.storageProcess = storageProcess;
        this.storageProxy = storageProxy;
        try {
            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // TODO: Consider adding FastPropertyManager class.
        // Set Fast Property callbacks for dynamic updates.
        DynamicPropertyFactory propertyFactory = DynamicPropertyFactory.getInstance();
        this.readConsistencyFP =
                propertyFactory.getStringProperty(
                        "florida.dyno.read.consistency", floridaConfig.getDynomiteReadConsistency());
        Runnable updateReadConsitencyFP = ()-> {
            logger.info("Updating FP: " + this.readConsistencyFP.getName());
            if (!DynomiteRest.sendCommand("/set_consistency/read/" + floridaConfig.getDynomiteReadConsistency())) {
                logger.error("REST call to Dynomite for read consistency failed --> using the default");
            }
        };
        this.readConsistencyFP.addCallback(updateReadConsitencyFP);

        this.writeConsistencyFP =
                propertyFactory.getStringProperty(
                        "florida.dyno.write.consistency", floridaConfig.getDynomiteWriteConsistency());
        Runnable updateWriteConsitencyFP = ()-> {
            logger.info("Updating FP: " + this.writeConsistencyFP.getName());
            if (!DynomiteRest.sendCommand("/set_consistency/write/" + floridaConfig.getDynomiteWriteConsistency())) {
                logger.error("REST call to Dynomite for write consistency failed --> using the default");
            }
        };
        this.writeConsistencyFP.addCallback(updateWriteConsitencyFP);

        DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(state));
    }

    public void initialize() throws Exception {
        if (id.getInstance().isOutOfService()) {
            logger.error("Out of service");
            return;
        }

        logger.info("Initializing Florida Server now ...");

        state.setSideCarProcessAlive(true);
        state.setBootstrapStatus(Bootstrap.NOT_STARTED);
        state.setStorageAlive(storageProxy.isAlive());

        if (floridaConfig.isDynomiteMultiDC()) {
            scheduler.runTaskNow(UpdateSecuritySettings.class);
            /*
             * sleep for some random between 100 - 200 sec if this is a new node
             * with new IP for SG to be updated by other seed nodes
             */
            if (id.isReplace() || id.isTokenPregenerated()) {
                long initTime = 100 + (int) (Math.random() * ((200 - 100) + 1));

                logger.info("Sleeping " + initTime + " seconds -> a node is replaced or token is pregenerated.");
                sleeper.sleep(initTime * 1000);
            } else if (UpdateSecuritySettings.firstTimeUpdated) {
                logger.info("Sleeping 60 seconds -> first time security settings are updated");
                sleeper.sleep(60 * 1000);
            }

            scheduler.addTask(UpdateSecuritySettings.JOBNAME, UpdateSecuritySettings.class,
                    UpdateSecuritySettings.getTimer(id));
        }

        // Invoking the task directly as any errors in this task
        // should not let Florida continue. However, we don't want to kill
        // the Florida process, but, want it to be stuck.
        logger.info("Running TuneTask and updating configuration.");
        try {
            tuneTask.execute();
        } catch (IOException e) {
            logger.error("Cannot update Dynomite YAML " + e.getMessage());
        }

        // Determine if we need to restore from backup else start Dynomite.
        if (commonConfig.isRestoreEnabled()) {
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
            if (floridaConfig.isForceWarm()) {
                logger.info("force bootstrap -> warm up");
                warmUp = true;
            } else if (floridaConfig.isWarmBootstrap() && id.isReplace()) {
                logger.info("Instance replacement -> warm up");
                warmUp = true;
            } else if (floridaConfig.isWarmBootstrap() && !id.isNewToken() && !storageProxy.isAlive()) {
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
        if (commonConfig.isBackupEnabled() && commonConfig.getBackupHour() >= 0) {
            scheduler.addTask(SnapshotTask.TaskName, SnapshotTask.class, SnapshotTask.getTimer(commonConfig));
        }

        // Metrics
        scheduler.addTask(ServoMetricsTask.TaskName, ServoMetricsTask.class, ServoMetricsTask.getTimer());
        scheduler.addTask(RedisInfoMetricsTask.TaskName, RedisInfoMetricsTask.class, RedisInfoMetricsTask.getTimer());

        // Routine monitoring and restarting dynomite or storage processes as
        // needed.
        scheduler.addTask(ProcessMonitorTask.JOBNAME, ProcessMonitorTask.class, ProcessMonitorTask.getTimer());
        scheduler.addTask(DynomiteProcessManager.JOB_TASK_NAME, DynomiteProcessManager.class,
                DynomiteProcessManager.getTimer());

        scheduler.addTask(RedisStorageProxy.JOB_TASK_NAME, RedisStorageProxy.class, RedisStorageProxy.getTimer());

        // Routing changing the YML file so that a manual Dynomite restart gets
        // the proper tokens
        scheduler.addTask(DynomiteYamlTask.JOBNAME, DynomiteYamlTask.class, DynomiteYamlTask.getTimer());

        logger.info("Starting task scheduler");
        scheduler.start();
    }

    public InstanceIdentity getId() {
        return id;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public FloridaConfig getConfiguration() {
        return floridaConfig;
    }

}
