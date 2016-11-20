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
package com.netflix.dynomitemanager.sidecore.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.identity.AppsInstance;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import com.netflix.dynomitemanager.dynomite.DynomiteRest;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Warm up the node's storage (i.e. Redis) by syncing data from a peer.
 */
@Singleton
public class WarmBootstrapTask extends Task {

    private static final Logger logger = LoggerFactory.getLogger(WarmBootstrapTask.class);

    public static final String JOBNAME = "Bootstrap-Task";
    private final IDynomiteProcess dynProcess;
    private final IStorageProxy storageProxy;
    private final IAppsInstanceFactory appsInstanceFactory;
    private final InstanceIdentity ii;
    private final InstanceState state;
    private final Sleeper sleeper;
    private final StorageProcessManager storageProcessMgr;

    @Inject
    public WarmBootstrapTask(IConfiguration config, IAppsInstanceFactory appsInstanceFactory, InstanceIdentity id,
	    IDynomiteProcess dynProcess, IStorageProxy storageProxy, InstanceState ss, Sleeper sleeper,
	    StorageProcessManager storageProcessMgr) {
	super(config);
	this.dynProcess = dynProcess;
	this.storageProxy = storageProxy;
	this.appsInstanceFactory = appsInstanceFactory;
	this.ii = id;
	this.state = ss;
	this.sleeper = sleeper;
	this.storageProcessMgr = storageProcessMgr;
    }

    public void execute() throws IOException {
	logger.info("Running warmbootstrapping ...");
	this.state.setFirstBootstrap(false);
	this.state.setBootstrapTime(DateTime.now());

	// Just to be sure testing again
	if (!state.isStorageAlive()) {
	    // starting storage
	    this.storageProcessMgr.start();
	    logger.info("Redis is up ---> Starting warm bootstrap.");

	    // setting the status to bootstrapping
	    this.state.setBootstrapping(true);

	    // sleep to make sure storage process is up
	    this.sleeper.sleepQuietly(5000);

	    String[] peers = getLocalPeersWithSameTokensRange();

	    // try one node only for now
	    // TODOs: if this peer is not good, try the next one until we can
	    // get the data
	    if (peers != null && peers.length != 0) {

		/**
		 * Check the warm up status.
		 */
		Bootstrap bootstrap = this.storageProxy.warmUpStorage(peers);
		if (bootstrap == Bootstrap.IN_SYNC_SUCCESS || bootstrap == Bootstrap.EXPIRED_BOOTSTRAPTIME_FAIL
			|| bootstrap == Bootstrap.RETRIES_FAIL) {
		    // Since we are ready let us start Dynomite.
		    try {
			this.dynProcess.start();
		    } catch (IOException ex) {
			logger.error("Dynomite failed to start");
		    }
		    // Wait for 1 second before we check dynomite status
		    sleeper.sleepQuietly(1000);
		    if (this.dynProcess.dynomiteCheck()) {
			logger.info("Dynomite health check passed");

			// Set the state of bootstrap as successful.
			this.state.setBootstrapStatus(bootstrap);

			logger.info("Set Dynomite to allow writes only!!!");
			DynomiteRest.sendCommand("/state/writes_only");

			logger.info("Stop Redis' Peer syncing!!!");
			this.storageProxy.stopPeerSync();

			logger.info("Set Dynomite to resuming state to allow writes and flush delayed writes");
			DynomiteRest.sendCommand("/state/resuming");

			// sleep 15s for the flushing to catch up
			sleeper.sleepQuietly(15000);
			logger.info("Set Dynomite to normal state");
			DynomiteRest.sendCommand("/state/normal");
		    } else {
			logger.error("Dynomite health check and restart attempts failed");
		    }
		} else {
		    logger.error("Warm up failed: Stop Redis' Peer syncing!!!");
		    this.storageProxy.stopPeerSync();
		}

	    } else {
		logger.error("Unable to find any peer with the same token!");
	    }

	    // Performing a check of Dynomite after bootstrap is complete. This
	    // is important as there are
	    // cases that Dynomite reaches the 1M messages limit and is
	    // inaccessible after bootstrap.
	    if (this.dynProcess.dynomiteCheck()) {
		logger.error("Dynomite is up since warm up succeeded");
	    }
	    // finalizing bootstrap
	    this.state.setBootstrapping(false);
	}
    }

    @Override
    public String getName() {
	return JOBNAME;
    }

    public static TaskTimer getTimer() {
	// run once every 10mins
	return new SimpleTimer(JOBNAME, 10 * 60 * 1000);
    }

    private String[] getLocalPeersWithSameTokensRange() {
	String tokens = ii.getTokens();

	logger.info("Warming up node's own token(s) : " + tokens);
	List<AppsInstance> instances = appsInstanceFactory.getLocalDCIds(config.getDynomiteClusterName(), config.getDataCenter());
	List<String> peers = new ArrayList<String>();

	for (AppsInstance ins : instances) {
            logger.info("Instance's token(s): " + ins.getToken() + " id: " + ins.getInstanceId());
	    if (!ins.getRack().equals(ii.getInstance().getRack()) && ins.getToken().equals(tokens)) {
		peers.add(ins.getHostName());
	    }
	}
	logger.info("peers size: " + peers.size());
	return peers.toArray(new String[0]);
    }

}
