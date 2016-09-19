/**
 * Copyright 2016 Netflix, Inc. <p/> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p/>
 * http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.netflix.dynomitemanager.monitoring.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.monitoring.JedisFactory;
import com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask;
import com.netflix.servo.DefaultMonitorRegistry;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import redis.clients.jedis.Jedis;

/**
 * Tests for RedisInfoMetricsTask
 *
 * @author diegopacheco
 *
 */
@RunWith(JMockit.class)
public class RedisInfoMetricsTaskTest {

	@Mocked(methods = { "connect", "disconnect", "info" }) Jedis jedis;

	@Test
	public void executeTest() throws Exception {

		int metricsCountSampleRedisInfo = 26;

		File file = new File(new File(".").getCanonicalPath() + "/src/test/resources/redis_info.txt");
		final String info = new String(Files.readAllBytes((Paths.get(file.getPath()))));

		new Expectations() {{
			jedis.connect();
			jedis.info();
			result = info;
			jedis.disconnect();
		}};

		JedisFactory jedisFactory = new JedisFactory() {
			@Override
			public Jedis newInstance() {
				return jedis;
			}
		};

		IConfiguration iConfig = new BlankConfiguration();

		RedisInfoMetricsTask mimt = new RedisInfoMetricsTask(iConfig, jedisFactory);
		mimt.execute();

		Assert.assertNotNull(DefaultMonitorRegistry.getInstance().getRegisteredMonitors());
		Assert.assertEquals(metricsCountSampleRedisInfo,
				DefaultMonitorRegistry.getInstance().getRegisteredMonitors().size());

	}

}
