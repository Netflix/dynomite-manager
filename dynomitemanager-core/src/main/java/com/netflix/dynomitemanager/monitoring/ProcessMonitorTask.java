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
package com.netflix.dynomitemanager.monitoring;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.config.InstanceState;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.storage.JedisUtils;
import com.netflix.dynomitemanager.storage.StorageProxy;
import com.netflix.nfsidecar.scheduler.SimpleTimer;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.scheduler.TaskTimer;

import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static java.util.concurrent.TimeUnit.MICROSECONDS;

/**
 * This is the only class that initiates starting and stopping
 * storage proxy, and storage processes. Hence, the updating of InstanceState
 * only happens on one thread, however, it can be read by multiple threads.
 * It is very important that it stays this way to keep process management
 * simple and in one place.
 *
 * Monitors Dynomite and Storage process and handles different
 * process failure scenarios.
 *
 * This class in cooperates with {@link com.netflix.dynomitemanager.storage.WarmBootstrapTask},
 * {@link com.netflix.dynomitemanager.storage.StorageProxy}, {@link com.netflix.dynomitemanager.dynomite.IDynomiteProcess},
 * and {@link com.netflix.dynomitemanager.config.InstanceState}.
 * to handle the following cases.
 *
 * Case 1: Redis dies
 * Case 2:
 *        a. Dynomite dies or
 *        b. Dynomite process has hung
 * Case 3: Florida dies (with or without one of the other two processes being dead)
 * Case 4: Florida and dynomite dies
 * Case 5: Redis + Dynomite dies
 * Case 6: Redis + Dynomite + Florida dies
 *
 * Currently the storage (Redis specifically) is launched by the dynomite launch script.
 * TODO: The Redis could be directly launched from Dynomite.
 *
 * @author Monal Daxini
 * @author Minh Do
 * @author ipapapa
 */
@Singleton
public class ProcessMonitorTask extends Task implements StatefulJob {

    public static final String JOBNAME = "DYNOMITE_PROCESS_MONITOR_THREAD";
    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitorTask.class);
    private final FloridaConfig config;
    private InstanceState instanceState;
    private final StorageProxy storageProxy;
    private final IDynomiteProcess dynomiteProcess;

    @Inject
    protected ProcessMonitorTask(FloridaConfig config, InstanceState instanceState,
                                 StorageProxy storageProxy, IDynomiteProcess dynomiteProcess) {
        this.config = config;
        this.instanceState = instanceState;
        this.storageProxy = storageProxy;
        this.dynomiteProcess = dynomiteProcess;
    }

    @Override
    public void execute() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (instanceState.getIsProcessMonitoringSuspended()) {
        	return;
        }
        
        logger.info("Healthy " + instanceState.isHealthy());
        
        instanceState.setStorageProxyProcessAlive(this.dynomiteProcess.dynomiteProcessCheck());
        instanceState.setStorageProxyAlive(JedisUtils.isAliveWithRetry(config.getDynomiteLocalAddress(), config.getDynomiteClientPort()));            
        instanceState.setStorageAlive(storageProxy.isAlive());
        logger.info(String.format("ProcessMonitor state: %s, time elapsted to check (micros): %s",
                        instanceState, stopwatch.elapsed(MICROSECONDS)));

        if((!instanceState.isStorageProxyProcessAlive())) {
            if (!instanceState.isStorageAlive()) {
                logger.error("FATAL: Redis is down.");
                // TODO: Take appropriate action.
            }
            else {
                logger.info("Detected Dynomite process is not running. Restarting dynomite.");
            }
            dynomiteProcess.start();
        }
        /*
        if((!instanceState.isStorageProxyAlive() && instanceState.isStorageProxyProcessAlive())) {
            if (!instanceState.isStorageAlive()) {
                logger.info("Stopping dynomite process isStorageAlive=false. Restarting dynomite will restart storage");
            }
            else {
                logger.info("Stopping hung dynomite process.");
            }
            dynProcess.stop();
        }
        */

        /*
        if (instanceState.isBootstrapping()) {
            logger.info("Instance is bootstrapping. Skipping further process checks.");
            return;
        }


        if (!instanceState.isStorageAlive()) {
            if(instanceState.isStorageProxyAlive() || instanceState.isStorageProxyProcessAlive()) {
                logger.info("Stopping Dynomite process before warm bootstrapping.");
                dynProcess.stop();
            }

            if (config.isWarmBootstrap()) {
                logger.info("Warm bootstraping node. Scheduling BootstrapTask now!");
                scheduler.runTaskNow(WarmBootstrapTask.class);
            }
            else {
                logger.info("Cold bootstraping, launching dynomite and storage process.");
                dynProcess.start(true);
            }

            logger.info(String.format("After corrective action ProcessMonitor state: %s, time elapsed to check (micros): %s",
                    instanceState, stopwatch.elapsed(MICROSECONDS)));
        }
        else if(!instanceState.isStorageProxyAlive()) {
            logger.info("Launching dynomite process.");
            // starts launch dynomite script, which starts Redis if it's not already running.
            dynProcess.start(true);

            logger.info(String.format("After corrective action ProcessMonitor state: %s, time elapsted to check (micros): %s",
                instanceState, stopwatch.elapsed(MICROSECONDS)));
        }
        */

        stopwatch.stop();

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Time to run the check (micros): %s", stopwatch.elapsed(MICROSECONDS)));
        }
    }


    // Start every 15 seconds.
    public static TaskTimer getTimer()
    {
        return new SimpleTimer(JOBNAME, 15L * 1000);
    }

    @Override
    public String getName()
    {
        return JOBNAME;
    }

}
