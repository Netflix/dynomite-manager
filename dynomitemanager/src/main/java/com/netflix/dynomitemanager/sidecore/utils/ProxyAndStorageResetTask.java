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
package com.netflix.dynomitemanager.sidecore.utils;

import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;

import static com.netflix.dynomitemanager.defaultimpl.DynomiteManagerConfiguration.LOCAL_ADDRESS;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.dynomite.DynomiteRest;

/**
 * Stop Redis replication, change Redis from slave to master, and restart
 * dynomite (if necessary).
 *
 * @author ipapapa
 */
@Singleton
public class ProxyAndStorageResetTask extends Task {
    public static final String JOBNAME = "ProxyResetTask-Task";
    private static final Logger logger = LoggerFactory.getLogger(ProxyAndStorageResetTask.class);

    private final IDynomiteProcess dynProcess;
    private final IStorageProxy storageProxy;
    private final Sleeper sleeper;

    @Inject
    public ProxyAndStorageResetTask(IConfiguration config, IDynomiteProcess dynProcess, IStorageProxy storageProxy,
	    Sleeper sleeper) {
	super(config);
	this.storageProxy = storageProxy;
	this.dynProcess = dynProcess;
	this.sleeper = sleeper;
    }

    public void execute() throws IOException {
	storageProxy.resetStorage();
	dynomiteCheck();
	setConsistency();
    }

    @Override
    public String getName() {
	return JOBNAME;
    }

    private void setConsistency() {
	logger.info("Setting the consistency level for the cluster");
	if (!DynomiteRest.sendCommand("/set_consistency/read/" + config.getReadConsistency()))
	    logger.error("REST call to Dynomite for read consistency failed --> using the default");

	if (!DynomiteRest.sendCommand("/set_consistency/write/" + config.getReadConsistency()))
	    logger.error("REST call to Dynomite for write consistency failed --> using the default");
    }

    private void dynomiteCheck() {
        Jedis dynomiteJedis = new Jedis(LOCAL_ADDRESS, config.getDynomiteClientPort(), 5000);
	logger.info("Checking Dynomite's status");
	try {
	    dynomiteJedis.connect();
	    if (dynomiteJedis.ping().equals("PONG") == false) {
		logger.warn("Pinging Dynomite failed ---> trying again after 1 sec");
		sleeper.sleepQuietly(1000);
		if (dynomiteJedis.ping().equals("PONG") == false) {
		    try {
			this.dynProcess.stop();
			sleeper.sleepQuietly(1000);
			this.dynProcess.start();
		    } catch (IOException e) {
			logger.error("Dynomite cannot be restarted --> Requires manual restart" + e.getMessage());
		    }
		} else {
		    logger.info("Dynomite is up and running");
		}
	    } else {
		logger.info("Dynomite is up and running");
	    }
	} catch (Exception e) {
	    logger.warn("Unable to connect to Dynomite --> restarting: " + e.getMessage());
	    try {
		this.dynProcess.stop();
		sleeper.sleepQuietly(1000);
		this.dynProcess.start();
	    } catch (IOException e1) {
		logger.error("Dynomite cannot be restarted --> Requires manual restart" + e1.getMessage());
	    }
	}

    }
}
