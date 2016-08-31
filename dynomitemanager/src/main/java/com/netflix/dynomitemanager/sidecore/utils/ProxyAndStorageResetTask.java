/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.utils;

import com.netflix.dynomitemanager.defaultimpl.JedisConfiguration;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;


@Singleton
public class ProxyAndStorageResetTask extends Task {
	public static final String JOBNAME = "ProxyResetTask-Task";
	private static final Logger logger = LoggerFactory.getLogger(ProxyAndStorageResetTask.class);


	private final IFloridaProcess dynProcess;
	private final IStorageProxy storageProxy;
	private final Sleeper sleeper;

	@Inject
	public ProxyAndStorageResetTask(IConfiguration config,
									IFloridaProcess dynProcess, IStorageProxy storageProxy, Sleeper sleeper) {
		super(config);
		this.storageProxy = storageProxy;
		this.dynProcess = dynProcess;
		this.sleeper = sleeper;
	}

	public void execute() throws IOException {
		storageProxy.resetStorage();
		dynomiteCheck();
	}

	@Override
	public String getName() {
		return JOBNAME;
	}

	private void dynomiteCheck() {
		if (config.getClusterType() == JedisConfiguration.DYNO_MEMCACHED) {    // TODO: we need to implement this once we use memcached
			logger.error("Memcache Dynomite check is not functional");
		} else if (config.getClusterType() == JedisConfiguration.DYNO_REDIS) {  //use Redis API
			Jedis dynomiteJedis = new Jedis(JedisConfiguration.REDIS_ADDRESS, JedisConfiguration.DYNO_PORT, 5000);
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
}
