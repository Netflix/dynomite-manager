package com.netflix.dynomitemanager.dynomite;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.storage.StorageProxy;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.utils.Sleeper;

@Singleton
public class ProxyAndStorageResetTask extends Task {
    public static final String JOBNAME = "ProxyResetTask-Task";
    private static final Logger logger = LoggerFactory.getLogger(ProxyAndStorageResetTask.class);

    private final IDynomiteProcess dynProcess;
    private final StorageProxy storageProxy;
    private final Sleeper sleeper;
    private final FloridaConfig config;
    private final DynomiteRest dynoRest;

    @Inject
    public ProxyAndStorageResetTask(FloridaConfig config, IDynomiteProcess dynProcess, StorageProxy storageProxy,
            Sleeper sleeper, DynomiteRest dynoRest) {
        this.config = config;
        this.storageProxy = storageProxy;
        this.dynProcess = dynProcess;
        this.sleeper = sleeper;
        this.dynoRest = dynoRest;
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
        if (!dynoRest.sendCommand("/set_consistency/read/" + config.getDynomiteReadConsistency()))
            logger.error("REST call to Dynomite for read consistency failed --> using the default");

        if (!dynoRest.sendCommand("/set_consistency/write/" + config.getDynomiteWriteConsistency()))
            logger.error("REST call to Dynomite for write consistency failed --> using the default");
    }

    private void dynomiteCheck() {
        Jedis dynomiteJedis = new Jedis(config.getDynomiteLocalAddress(), config.getDynomiteClientPort(), 5000);
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
        } finally {
        	dynomiteJedis.close();
        }

    }
}
