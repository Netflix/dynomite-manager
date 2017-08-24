package com.netflix.florida.sidecore.monitoring.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.florida.config.FloridaConfig;
import com.netflix.florida.defaultimpl.test.BlankConfiguration;
import com.netflix.florida.defaultimpl.test.FakeStorageProxy;
import com.netflix.florida.monitoring.JedisFactory;
import com.netflix.florida.monitoring.RedisInfoMetricsTask;
import com.netflix.florida.sidecore.storage.StorageProxy;
import com.netflix.servo.DefaultMonitorRegistry;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import redis.clients.jedis.Jedis;

@RunWith(JMockit.class)
public class RedisInfoMetricsTaskTest {

    @Mocked
    Jedis jedis;

    @Test
    public void executeTest() throws Exception {

        int metricsCountSampleRedisInfo = 26;

        File file = new File(new File(".").getCanonicalPath() + "/src/test/resources/redis_info.txt");
        final String info = new String(Files.readAllBytes((Paths.get(file.getPath()))));

        new Expectations() {
            {
                jedis.connect();
                jedis.info();
                result = info;
                jedis.disconnect();
            }
        };

        JedisFactory jedisFactory = new JedisFactory() {

            @Override
            public Jedis newInstance(String hostname, int port) {
                return jedis;
            }
        };       

        StorageProxy storageProxy = new FakeStorageProxy();

        RedisInfoMetricsTask mimt = new RedisInfoMetricsTask(storageProxy, jedisFactory);
        mimt.execute();

        Assert.assertNotNull(DefaultMonitorRegistry.getInstance().getRegisteredMonitors());
        Assert.assertEquals(metricsCountSampleRedisInfo,
                DefaultMonitorRegistry.getInstance().getRegisteredMonitors().size());

    }
}
