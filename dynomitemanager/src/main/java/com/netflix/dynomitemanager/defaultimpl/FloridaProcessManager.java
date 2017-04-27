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
package com.netflix.dynomitemanager.defaultimpl;

import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.DYNO_MEMCACHED;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.DYNO_PORT;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.DYNO_REDIS;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.LOCAL_ADDRESS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
public class FloridaProcessManager implements IFloridaProcess
{
    private static final Logger logger = LoggerFactory.getLogger(FloridaProcessManager.class);
    private static final String SUDO_STRING = "/usr/bin/sudo";
    private static final int SCRIPT_EXECUTE_WAIT_TIME_MS = 5000;
    private final IConfiguration config;
    private final Sleeper sleeper;
    private final InstanceState instanceState;
    private final IFloridaProcess dynProcess;


    @Inject
    public FloridaProcessManager(IConfiguration config, Sleeper sleeper, InstanceState instanceState,
    		IFloridaProcess dynProcess)
    {
        this.config = config;
        this.sleeper = sleeper;
        this.instanceState = instanceState;
        this.dynProcess = dynProcess;
    }

    protected void setEnv(Map<String, String> env) {   
        env.put("MBUF_SIZE", String.valueOf(config.getDynomiteMbufSize()));
        if (config.getDynomiteMaxAllocatedMessages() > 0) {
            env.put("ALLOC_MSGS",String.valueOf(config.getDynomiteMaxAllocatedMessages()));
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
        Map<String, String> env = startDynomite.environment();
        setEnv(env);

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

    protected List<String> getStartCommand()
    {
        List<String> startCmd = new LinkedList<String>();
        for(String param : config.getDynomiteStartScript().split(" ")){
            if( StringUtils.isNotBlank(param))
                startCmd.add(param);
        }
        return startCmd;
    }

    void logProcessOutput(Process p)
    {
        try
        {
            final String stdOut = readProcessStream(p.getInputStream());
            final String stdErr = readProcessStream(p.getErrorStream());
            logger.info("std_out: {}", stdOut);
            logger.info("std_err: {}", stdErr);
        }
        catch(IOException ioe)
        {
            logger.warn("Failed to read the std out/err streams", ioe);
        }
    }

    String readProcessStream(InputStream inputStream) throws IOException
    {
        final byte[] buffer = new byte[512];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);
        int cnt;
        while ((cnt = inputStream.read(buffer)) != -1)
            baos.write(buffer, 0, cnt);
        return baos.toString();
    }


    public void stop() throws IOException
    {
        logger.info("Stopping Dynomite server ....");
        List<String> command = Lists.newArrayList();
        if (!"root".equals(System.getProperty("user.name")))
        {
            command.add(SUDO_STRING);
            command.add("-n");
            command.add("-E");
        }
        for(String param : config.getDynomiteStopScript().split(" ")){
            if( StringUtils.isNotBlank(param))
                command.add(param);
        }
        ProcessBuilder stopCass = new ProcessBuilder(command);
        stopCass.directory(new File("/"));
        stopCass.redirectErrorStream(true);
        Process stopper = stopCass.start();

        sleeper.sleepQuietly(SCRIPT_EXECUTE_WAIT_TIME_MS);
        try
        {
            int code = stopper.exitValue();
            if (code == 0) {
                logger.info("Dynomite server has been stopped");
                instanceState.setStorageProxyAlive(false);
            }
            else
            {
                logger.error("Unable to stop Dynomite server. Error code: {}", code);
                logProcessOutput(stopper);
            }
        }
        catch(Exception e)
        {
            logger.warn("couldn't shut down Dynomite correctly", e);
        }
    }
    
    /* Dynomite Redis Ping */
    private boolean dynomiteRedisPing(Jedis dynomiteJedis){
		if (dynomiteJedis.ping().equals("PONG") == false) {
			logger.warn("Pinging Dynomite failed");
			return false;
		}
		logger.info("Dynomite is up and running");
		return true;
    }
   
    
    /* Dynomite Healthcheck with Redis */
    private boolean dynomiteRedisCheck(){
    	Jedis dynomiteJedis = new Jedis(LOCAL_ADDRESS, DYNO_PORT, 5000);
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
        	
    /* Dynomite Healthcheck and Auto Restart */
    public boolean dynomiteCheck(){
    	if (config.getClusterType() == DYNO_MEMCACHED){    // TODO: we need to implement this once we use memcached
    		logger.error("Dynomite check with Memcached ping is not functional");
    	}
    	else if (config.getClusterType() == DYNO_REDIS) {  //use Redis API
        	logger.info("Dynomite check with Redis Ping");
        	if(!dynomiteRedisCheck()) {
        		try{
            		logger.error("Dynomite was down");
        			this.dynProcess.stop();
        			sleeper.sleepQuietly(1000);
        			return false;
            	} catch (IOException e) {
            		logger.error("Dynomite cannot be restarted --> Requires manual restart" + e.getMessage());
            	}
        	}
    	}
    	
    	return true;
    }
}
