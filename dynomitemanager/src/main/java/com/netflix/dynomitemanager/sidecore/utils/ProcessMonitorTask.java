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
package com.netflix.dynomitemanager.sidecore.utils;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.JedisConfiguration;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskScheduler;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;

import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
 * This class in cooperates with {@link com.netflix.dynomitemanager.sidecore.utils.WarmBootstrapTask},
 * {@link com.netflix.dynomitemanager.sidecore.storage.IStorageProxy}, {@link com.netflix.dynomitemanager.IFloridaProcess},
 * and {@link com.netflix.dynomitemanager.InstanceState}.
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
 */
@Singleton
public class ProcessMonitorTask extends Task implements StatefulJob {

    public static final String JOBNAME = "DYNOMITE_PROCESS_MONITOR_THREAD";
    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitorTask.class);
    private final IConfiguration config;
    private final InstanceState instanceState;
    private final IStorageProxy iStorageProxy;
    private final TaskScheduler scheduler;
    private final IFloridaProcess dynProcess;

    @Inject
    protected ProcessMonitorTask(IConfiguration config, InstanceState instanceState,
                                 IStorageProxy iStorageProxy, TaskScheduler scheduler, IFloridaProcess dynProcess) {
        super(config);
        this.config = config;
        this.instanceState = instanceState;
        this.iStorageProxy = iStorageProxy;
        this.scheduler = scheduler;
        this.dynProcess = dynProcess;
    }

    @Override
    public void execute() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (instanceState.getIsProcessMonitoringSuspended()) {
        	return;
        }
        
        instanceState.setStorageProxyProcessAlive(checkProxyProcess());
        instanceState.setStorageProxyAlive(JedisUtils.isAliveWithRetry(JedisConfiguration.REDIS_ADDRESS, JedisConfiguration.DYNO_PORT));            
        instanceState.setStorageAlive(iStorageProxy.isAlive());
        logger.info(String.format("ProcessMonitor state: %s, time elapsted to check (micros): %s",
                        instanceState, stopwatch.elapsed(MICROSECONDS)));
        

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

    private boolean checkProxyProcess() {
        try
        {
            String cmd = String.format("ps -ef | grep  '[/]apps/%1$s/bin/%1$s'", config.getProcessName());
            String[] cmdArray = {"/bin/sh", "-c", cmd};
            logger.info("Running checkProxyProcess command: " + cmd);

            // This returns pid for the Dynomite process
            Process p = Runtime.getRuntime().exec(cmdArray);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = input.readLine();
            if (logger.isDebugEnabled()) {
                logger.debug("Output from checkProxyProcess command: " + line);
            }
            return line != null;
        }
        catch(Exception e)
        {
            logger.warn("Exception thrown while checking if the process is running or not ", e);
            return false;
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
