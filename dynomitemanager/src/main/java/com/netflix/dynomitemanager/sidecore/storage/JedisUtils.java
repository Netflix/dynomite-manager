package com.netflix.dynomitemanager.sidecore.storage;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.dynomitemanager.sidecore.utils.BoundedExponentialRetryCallable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * Useful utilities to connect to storage or storage proxy via Jedis.
 *
 * @author Monal Daxini
 * @author ipapapa
 */
public class JedisUtils {
    private static final Logger logger = LoggerFactory.getLogger(JedisUtils.class);

    private static final DynamicLongProperty minRetryMs = DynamicPropertyFactory.getInstance()
            .getLongProperty("florida.storage.isAlive.retry.min.ms", 3000L);

    private static final DynamicLongProperty maxRetryMs = DynamicPropertyFactory.getInstance()
            .getLongProperty("florida.storage.isAlive.retry.max.ms", 30000L);

    private static final DynamicIntProperty jedisConnectTimeoutMs = DynamicPropertyFactory.getInstance()
            .getIntProperty("florida.storage.jedis.connect.timeout.ms", 30000);

    /**
     * The caller is responsible for invoking
     * {@link redis.clients.jedis.Jedis#disconnect()}.
     *
     * @return a Jedis object connected to the specified host and port.
     */
    public static Jedis connect(final String host, final int port) {
        Jedis jedis;
        try {
            jedis = new Jedis(host, port, jedisConnectTimeoutMs.getValue());
            jedis.connect();
            return jedis;
        } catch (Exception e) {
            logger.warn("Unable to connect to host:" + host + " port: " + port);
        }

        return null;
    }

    /**
     * Sends a SETEX with an expire after 1 sec to Redis at the specified port
     * 
     * @return an OK response if everything was written properly
     */
    public static boolean isWritableWithRetry(final String host, final int port) {
        BoundedExponentialRetryCallable<Boolean> jedisRetryCallable = new BoundedExponentialRetryCallable<Boolean>() {
            Jedis jedis = null;

            @Override
            public Boolean retriableCall() throws Exception {
                jedis = connect(host, port);
                /*
                 * check 1: write a SETEX key (single write) and auto-expire
                 */
                String status = jedis.setex("ignore_dyno", 1, "dynomite");
                if (!status.equalsIgnoreCase("OK")) {
                    jedis.disconnect();
                    return false;
                }
                return true;

            }

            @Override
            public void forEachExecution() {
                jedis.disconnect();
            }
        };

        jedisRetryCallable.setMin(minRetryMs.getValue());
        jedisRetryCallable.setMax(maxRetryMs.getValue());

        try {
            return jedisRetryCallable.call();
        } catch (Exception e) {
            logger.warn(String.format("All retries to SETEX to host:%s port:%s failed.", host, port));
            return false;
        }

    }

    /**
     * Sends a PING and INFO to Dynomite and Redis at the specified host and
     * port.
     *
     * @return true if a PONG or found "master" in the role, else false.
     */
    public static boolean isAliveWithRetry(final String host, final int port) {

        BoundedExponentialRetryCallable<Boolean> jedisRetryCallable = new BoundedExponentialRetryCallable<Boolean>() {
            Jedis jedis = null;

            @Override
            public Boolean retriableCall() throws Exception {
                jedis = connect(host, port);
                /* check 1: perform a ping */
                if (jedis.ping() == null) {
                    jedis.disconnect();
                    return false;
                }
                jedis.disconnect();
                return true;
            }

            @Override
            public void forEachExecution() {
                jedis.disconnect();
            }
        };

        jedisRetryCallable.setMin(minRetryMs.getValue());
        jedisRetryCallable.setMax(maxRetryMs.getValue());

        try {
            return jedisRetryCallable.call();
        } catch (Exception e) {
            logger.warn(String.format("All retries to PING host:%s port:%s failed.", host, port));
            return false;
        }

    }
}
