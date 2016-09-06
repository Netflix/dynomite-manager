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

import com.netflix.config.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * Useful utilities to connect to storage or storage proxy via Jedis.
 *
 * @author Monal Daxini
 */
public class JedisUtils {
	private static final Logger logger = LoggerFactory.getLogger(JedisUtils.class);

	private static final DynamicLongProperty minRetryMs = DynamicPropertyFactory.getInstance()
			.getLongProperty("dynomitemanager.storage.isAlive.retry.min.ms", 3000L);

	private static final DynamicLongProperty maxRetryMs = DynamicPropertyFactory.getInstance()
			.getLongProperty("dynomitemanager.storage.isAlive.retry.max.ms", 30000L);

	private static final DynamicIntProperty jedisConnectTimeoutMs = DynamicPropertyFactory.getInstance()
			.getIntProperty("dynomitemanager.storage.jedis.connect.timeout.ms", 30000);

	/**
	 * The caller is responsible for invoking {@link redis.clients.jedis.Jedis#disconnect()}.
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
			logger.info("Unable to connect");
		}

		return null;
	}

	/**
	 * Sends a PING to the Redis server at the specified host and port.
	 *
	 * @return true if a PONG was received, else false.
	 */
	@SuppressWarnings("unused")
	public static boolean isAlive(final String host, final int port) {
		Jedis jedis = connect(host, port);
		boolean result = false;
		try {
			result = jedis.ping() != null;
		} catch (Exception e) {
			logger.info("host [" + host + "] at port[" + port + "] is not alive");
		} finally {
			jedis.disconnect();
		}

		return result;
	}

	/**
	 * Sends a PING to the server at the specified host and port.
	 *
	 * @return true if a PONG was received, else false.
	 */
	public static boolean isAliveWithRetry(final String host, final int port) {

		BoundedExponentialRetryCallable<Boolean> jedisRetryCallable = new BoundedExponentialRetryCallable<Boolean>() {
			Jedis jedis = null;

			@Override
			public Boolean retriableCall() throws Exception {
				jedis = connect(host, port);
				return jedis.ping() != null;
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
			logger.warn(String.format("All retries to connect to host:%s port:%s failed.", host, port));
			return false;
		}

	}
}
