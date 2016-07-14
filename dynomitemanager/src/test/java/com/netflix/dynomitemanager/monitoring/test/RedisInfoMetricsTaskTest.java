package com.netflix.dynomitemanager.monitoring.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.dynomitemanager.monitoring.JedisFactory;
import com.netflix.dynomitemanager.monitoring.RedisInfoMetricsTask;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
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
	
	@Mocked(methods={"connect","disconnect","info"})
	Jedis jedis;
	
	@Test
	public void executeTest() throws Exception{
		
		 File file = new File(new File(".").getCanonicalPath() + "/src/test/resources/redis_info.txt");
		 final String info = new String(Files.readAllBytes((Paths.get(file.getPath()))));
		
		new Expectations() {{
		   jedis.connect();
		   jedis.info(); result = info;
		   jedis.disconnect();
		}};
		
		JedisFactory jedisFactory = new JedisFactory() {
			@Override
			public Jedis newInstance() {
				return jedis;
			}
		};
		
		IConfiguration iConfig = new BlankConfiguration();
		
		RedisInfoMetricsTask mimt = new RedisInfoMetricsTask(iConfig,jedisFactory);
		mimt.execute();
		
		Assert.assertNotNull(DefaultMonitorRegistry.getInstance().getRegisteredMonitors());
		Assert.assertEquals(26, DefaultMonitorRegistry.getInstance().getRegisteredMonitors().size());
		
	}
	
}
