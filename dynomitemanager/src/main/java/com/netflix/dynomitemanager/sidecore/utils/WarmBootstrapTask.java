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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.JedisConfiguration;
import com.netflix.dynomitemanager.identity.AppsInstance;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import com.netflix.dynomitemanager.sidecore.utils.WarmBootstrapTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;


@Singleton
public class WarmBootstrapTask extends Task
{
    private static final Logger logger = LoggerFactory.getLogger(WarmBootstrapTask.class);
    
    public static final String JOBNAME = "Bootstrap-Task";
    private final IFloridaProcess dynProcess;
    private final IStorageProxy storageProxy;
    private final IAppsInstanceFactory appsInstanceFactory;
    private final InstanceIdentity ii;
    private final InstanceState state;
    private final Sleeper sleeper;

    @Inject
    public WarmBootstrapTask(IConfiguration config, IAppsInstanceFactory appsInstanceFactory,
                             InstanceIdentity id, IFloridaProcess dynProcess,
                             IStorageProxy storageProxy, InstanceState ss, Sleeper sleeper)
    {
        super(config);
        this.dynProcess = dynProcess;
        this.storageProxy = storageProxy;
        this.appsInstanceFactory = appsInstanceFactory;
        this.ii = id;
        this.state = ss;
        this.sleeper = sleeper;
    }

    public void execute() throws IOException
    {
        logger.info("Running warmbootstrapping ...");
        // Just to be sure testing again
        if (!state.isStorageAlive()) {
            logger.info("Redis is up ---> Starting warm bootstrap.");
            this.state.setBootstrapping(true);

            //start dynProcess if it is not running.
            this.dynProcess.start(false);
            //sleep to make sure Dynomite process is up, Storage process is up.
            this.sleeper.sleepQuietly(15000);
            
            String[] peers = getPeersWithSameTokensRange();
            
            //try one node only for now 
            //TODOs: if this peer is not good, try the next one until we can get the data
            if (peers != null && peers.length != 0) {
                this.storageProxy.warmUpStorage(peers);
            } else {
                logger.warn("Unable to find any peer for downstreaming!!!!");
            }
            
            /*
             * Performing a check of Dynomite after bootstrap is complete.
             * This is important as there are cases that Dynomite reaches
             * the 1M messages limit and is unaccessible after bootstrap.
             */
        	this.dynProcess.dynomiteCheck();
        	// finalizing bootstrap
            this.state.setBootstrapping(false);
        }
    }

    @Override
    public String getName()
    {
        return JOBNAME;
    }

    
    public static TaskTimer getTimer() {
        // run once every 10mins
        return new SimpleTimer(JOBNAME, 10* 60*1000);
    }
    
    private String[] getPeersWithSameTokensRange() {
        String tokens = ii.getTokens();
        
        logger.info("Warming up node's own token(s) : " + tokens);
        List<AppsInstance> instances = appsInstanceFactory.getAllIds(config.getAppName());
        List<String> peers = new ArrayList<String>();
        
        for(AppsInstance ins : instances) {
            logger.info("Instance's token(s); " + ins.getToken());
            if (!ins.getRack().equals(ii.getInstance().getRack()) &&
                    ins.getToken().equals(tokens)) {
                peers.add(ins.getHostName());
            }
        }
        logger.info("peers size: " + peers.size());
        return peers.toArray(new String[0]);
    }

    
}
