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
package com.netflix.dynomitemanager.dynomite;

import static com.netflix.dynomitemanager.defaultimpl.DynomiteManagerConfiguration.LOCAL_ADDRESS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;

import redis.clients.jedis.Jedis;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Singleton
public class DynomiteProcessManager implements IDynomiteProcess {

    private static final Logger logger = LoggerFactory.getLogger(DynomiteProcessManager.class);
    private static final String SUDO_STRING = "/usr/bin/sudo";
    private static final int SCRIPT_EXECUTE_WAIT_TIME_MS = 5000;
    private final IConfiguration config;
    private final Sleeper sleeper;
    private final InstanceState instanceState;
    private final IDynomiteProcess dynProcess;

    @Inject
    public DynomiteProcessManager(IConfiguration config, Sleeper sleeper, InstanceState instanceState,
	    IDynomiteProcess dynProcess) {
	this.config = config;
	this.sleeper = sleeper;
	this.instanceState = instanceState;
	this.dynProcess = dynProcess;
    }

    protected void setDynomiteEnv(Map<String, String> env) {
	env.put("MBUF_SIZE", String.valueOf(config.getMbufSize()));
	if (config.getAllocatedMessages() > 0) {
	    env.put("ALLOC_MSGS", String.valueOf(config.getAllocatedMessages()));
	}
    }

    public void start() throws IOException {
	logger.info(String.format("Starting dynomite server"));

	List<String> command = Lists.newArrayList();
	if (!"root".equals(System.getProperty("user.name"))) {
	    command.add(SUDO_STRING);
	    command.add("-n");
	    command.add("-E");
	}
	command.addAll(getStartCommand());

	ProcessBuilder startDynomite = new ProcessBuilder(command);
	setDynomiteEnv(startDynomite.environment());

	startDynomite.directory(new File("/"));
	startDynomite.redirectErrorStream(true);
	Process starter = startDynomite.start();

	try {
	    sleeper.sleepQuietly(SCRIPT_EXECUTE_WAIT_TIME_MS);
	    int code = starter.exitValue();
	    if (code == 0) {
		logger.info("Dynomite server has been started");
		instanceState.setStorageProxyAlive(true);
	    } else {
		logger.error("Unable to start Dynomite server. Error code: {}", code);
	    }

	    logProcessOutput(starter);
	} catch (Exception e) {
	    logger.warn("Starting Dynomite has an error", e);
	}
    }

    protected List<String> getStartCommand() {
	List<String> startCmd = new LinkedList<String>();
	for (String param : config.getDynomiteStartupScript().split(" ")) {
	    if (StringUtils.isNotBlank(param))
		startCmd.add(param);
	}
	return startCmd;
    }

    void logProcessOutput(Process p) {
	try {
	    final String stdOut = readProcessStream(p.getInputStream());
	    final String stdErr = readProcessStream(p.getErrorStream());
	    logger.info("std_out: {}", stdOut);
	    logger.info("std_err: {}", stdErr);
	} catch (IOException ioe) {
	    logger.warn("Failed to read the std out/err streams", ioe);
	}
    }

    String readProcessStream(InputStream inputStream) throws IOException {
	final byte[] buffer = new byte[512];
	final ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);
	int cnt;
	while ((cnt = inputStream.read(buffer)) != -1)
	    baos.write(buffer, 0, cnt);
	return baos.toString();
    }

    public void stop() throws IOException {
	logger.info("Stopping Dynomite server ....");
	List<String> command = Lists.newArrayList();
	if (!"root".equals(System.getProperty("user.name"))) {
	    command.add(SUDO_STRING);
	    command.add("-n");
	    command.add("-E");
	}
	for (String param : config.getDynomiteStopScript().split(" ")) {
	    if (StringUtils.isNotBlank(param))
		command.add(param);
	}
	ProcessBuilder stopCass = new ProcessBuilder(command);
	stopCass.directory(new File("/"));
	stopCass.redirectErrorStream(true);
	Process stopper = stopCass.start();

	sleeper.sleepQuietly(SCRIPT_EXECUTE_WAIT_TIME_MS);
	try {
	    int code = stopper.exitValue();
	    if (code == 0) {
		logger.info("Dynomite server has been stopped");
		instanceState.setStorageProxyAlive(false);
	    } else {
		logger.error("Unable to stop Dynomite server. Error code: {}", code);
		logProcessOutput(stopper);
	    }
	} catch (Exception e) {
	    logger.warn("couldn't shut down Dynomite correctly", e);
	}
    }

    /**
     * Ping Dynomite to perform a basic health check.
     *
     * @param dynomiteJedis
     *            the Jedis client with a connection to Dynomite.
     * @return true if Dynomite replies to PING with PONG, else false.
     */
    private boolean dynomiteRedisPing(Jedis dynomiteJedis) {
	if (dynomiteJedis.ping().equals("PONG") == false) {
	    logger.warn("Pinging Dynomite failed");
	    return false;
	}
	logger.info("Dynomite is up and running");
	return true;
    }

    /**
     * Basic health check for Dynomite.
     *
     * @return true if health check passes, or false if health check fails.
     */
    private boolean dynomiteRedisCheck() {
        Jedis dynomiteJedis = new Jedis(LOCAL_ADDRESS, config.getDynomiteClientPort(), 5000);
	try {
	    dynomiteJedis.connect();
	    if (!dynomiteRedisPing(dynomiteJedis)) {
		sleeper.sleepQuietly(1000);
		if (!dynomiteRedisPing(dynomiteJedis)) {
		    logger.warn("Second effort to ping Dynomite failed");
		    return false;
		}
	    }
	} catch (Exception e) {
	    logger.warn("Unable to create a Jedis connection to Dynomite" + e.getMessage());
	    return false;
	}
	return true;
    }

    /**
     * Dynomite health check performed via Redis PING command. If health check
     * fails, then we stop Dynomite to prevent a zombie Dynomite process (i.e. a
     * situation where Dynomite is running but Redis is stopped).
     *
     * @return true if health check passes and false if it fails.
     */
    public boolean dynomiteCheck() {
	logger.info("Dynomite check with Redis Ping");
	if (!dynomiteRedisCheck()) {
	    try {
		logger.error("Dynomite was down");
		this.dynProcess.stop();
		sleeper.sleepQuietly(1000);
		return false;
	    } catch (IOException e) {
		logger.error("Dynomite cannot be restarted --> Requires manual restart" + e.getMessage());
	    }
	}

	return true;
    }

}
