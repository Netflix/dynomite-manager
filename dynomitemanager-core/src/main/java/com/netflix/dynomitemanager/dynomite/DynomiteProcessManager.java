package com.netflix.dynomitemanager.dynomite;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.storage.JedisUtils;
import com.netflix.nfsidecar.identity.IInstanceState;
import com.netflix.nfsidecar.scheduler.SimpleTimer;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.scheduler.TaskTimer;
import com.netflix.nfsidecar.utils.Sleeper;
import com.netflix.runtime.health.api.Health;
import com.netflix.runtime.health.api.HealthIndicator;
import com.netflix.runtime.health.api.HealthIndicatorCallback;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import redis.clients.jedis.Jedis;

@Singleton
public class DynomiteProcessManager extends Task implements IDynomiteProcess, HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(DynomiteProcessManager.class);
    private static final String SUDO_STRING = "/usr/bin/sudo";
    public static final String JOB_TASK_NAME = "DYNOMITE HEALTH TRACKER";
    private static final int SCRIPT_EXECUTE_WAIT_TIME_MS = 5000;
    private final FloridaConfig config;
    private final Sleeper sleeper;
    private final IInstanceState instanceState;
    private final IDynomiteProcess dynProcess;
    private boolean dynomiteHealth = false;

    @Inject
    public DynomiteProcessManager(FloridaConfig config, Sleeper sleeper, IInstanceState instanceState,
            IDynomiteProcess dynProcess) {
        this.config = config;
        this.sleeper = sleeper;
        this.instanceState = instanceState;
        this.dynProcess = dynProcess;
    }

    public static TaskTimer getTimer() {
        return new SimpleTimer(JOB_TASK_NAME, 15L * 1000);
    }

    @Override
    public String getName() {
        return JOB_TASK_NAME;
    }

    @Override
    public void execute() throws Exception {
        dynomiteHealth = dynomiteProcessCheck()
                && JedisUtils.isAliveWithRetry(config.getDynomiteLocalAddress(), config.getDynomiteClientPort());
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
        for (String param : config.getDynomiteStartScript().split(" ")) {
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
        ProcessBuilder stopDyno = new ProcessBuilder(command);
        stopDyno.directory(new File("/"));
        stopDyno.redirectErrorStream(true);
        Process stopper = stopDyno.start();

        sleeper.sleepQuietly(SCRIPT_EXECUTE_WAIT_TIME_MS);
        try {
            int code = stopper.exitValue();
            if (code == 0) {
                logger.info("Dynomite server has been stopped");
                instanceState.setStorageProxyAlive(false);
            } else {
                logger.error("Unable to stop Dynomite server with script " + config.getDynomiteStopScript()
                        + " Error code: {}", code);
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
        boolean result = true;
        Jedis dynomiteJedis = new Jedis(config.getDynomiteLocalAddress(), config.getDynomiteClientPort(), 5000);
        try {
            dynomiteJedis.connect();
            if (!dynomiteRedisPing(dynomiteJedis)) {
                sleeper.sleepQuietly(1000);
                if (!dynomiteRedisPing(dynomiteJedis)) {
                    logger.warn("Second effort to ping Dynomite failed");
                    result = false;
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to create a Jedis connection to Dynomite" + e.getMessage());
            result = false;
        }
        dynomiteJedis.disconnect();
        return result;
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

    public boolean dynomiteProcessCheck() {
        Process process = null;
        try {
            String cmd = String.format("ps -ef | grep  '[/]apps/%1$s/bin/%1$s'", config.getDynomiteProcessName());
            String[] cmdArray = { "/bin/sh", "-c", cmd };
            logger.debug("Running checkProxyProcess command: " + cmd);

            // This returns pid for the Dynomite process
            process = Runtime.getRuntime().exec(cmdArray);
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = input.readLine();
            if (logger.isDebugEnabled()) {
                logger.debug("Output from checkProxyProcess command: " + line);
            }
            return line != null;
        } catch (Exception e) {
            logger.warn("Exception thrown while checking if the process is running or not ", e);
            return false;
        } finally {
            if (process != null) {
                IOUtils.closeQuietly(process.getInputStream());
                IOUtils.closeQuietly(process.getOutputStream());
                IOUtils.closeQuietly(process.getErrorStream());
            }
        }
    }

    public void check(HealthIndicatorCallback healthCallback) {
        if (dynomiteHealth) {
            healthCallback.inform(Health.healthy().withDetail("Dynomite", "All good!").build());
        } else {
            logger.info("Reporting Dynomite is down to Health check callback");
            healthCallback.inform(Health.unhealthy().withDetail("Dynomite", "Down!").build());
        }
    }

}
