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
package com.netflix.dynomitemanager.monitoring;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.storage.RedisInfoParser;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.NumericMonitor;

@Singleton
public class RedisInfoMetricsTask extends Task {

    private static final Logger Logger = LoggerFactory.getLogger(RedisInfoMetricsTask.class);

    private static final Set<String> COUNTER_LIST = new HashSet<String>();

    static {
        COUNTER_LIST.add("Redis_Stats_instantaneous_ops_per_sec");
    }

    // The Task name for identification
    public static final String TaskName = "Redis-Info-Task";

    private final ConcurrentHashMap<String, LongGauge> redisInfoGaugeMetrics = new ConcurrentHashMap<String, LongGauge>();
    private final ConcurrentHashMap<String, NumericMonitor<Number>> redisInfoCounterMap = new ConcurrentHashMap<String, NumericMonitor<Number>>();

    private JedisFactory jedisFactory;
    private IStorageProxy storageProxy;

    /**
     * Default constructor
     * 
     * @param config
     * @param storageProxy
     * @param jedisFactory
     */
    @Inject
    public RedisInfoMetricsTask(IConfiguration config, IStorageProxy storageProxy, JedisFactory jedisFactory) {
        super(config);
        this.jedisFactory = jedisFactory;
        this.storageProxy = storageProxy;
    }

    @Override
    public void execute() throws Exception {
        Jedis jedis = jedisFactory.newInstance(storageProxy.getIpAddress(),storageProxy.getPort());

        try {
            jedis.connect();
            String s = jedis.info();

            InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
            RedisInfoParser infoParser = new RedisInfoParser();
            Map<String, Long> metrics = infoParser.parse(reader);

            processMetrics(metrics);

        } catch (Exception e) {
            Logger.error("Could not get jedis info metrics", e);
        } finally {
            jedis.disconnect();
        }
    }

    private void processMetrics(Map<String, Long> metrics) {
        for (String key : metrics.keySet()) {

            Long value = metrics.get(key);

            if (COUNTER_LIST.contains(key)) {
                processCounterMetric(key, value);
            } else {
                processGaugeMetric(key, value);
            }
        }
    }

    private void processGaugeMetric(String key, Long value) {
        if (Logger.isDebugEnabled()) {
            Logger.debug("Process gauge: " + key + " " + value);
        }

        LongGauge oldGauge = redisInfoGaugeMetrics.get(key);
        if (oldGauge != null) {
            oldGauge.set(value);
            return;
        }

        // create a new long gauge
        LongGauge newGauge = new LongGauge(MonitorConfig.builder(key).build());

        oldGauge = redisInfoGaugeMetrics.putIfAbsent(key, newGauge);
        if (oldGauge == null) {
            newGauge.set(value);
            DefaultMonitorRegistry.getInstance().register(newGauge);
        } else {
            // someone else beat us to it. just use the oldGauge
            oldGauge.set(value);
        }
    }

    private void processCounterMetric(String counterName, Long val) {

        if (Logger.isDebugEnabled()) {
            Logger.debug("Process counter: " + counterName + " " + val);
        }

        NumericMonitor<Number> counter = redisInfoCounterMap.get(counterName);
        if (counter != null) {
            long increment = val - counter.getValue().longValue();
            ((Counter) counter).increment(increment);
            return;
        }

        counter = Monitors.newCounter(counterName);
        NumericMonitor<Number> oldCounter = redisInfoCounterMap.putIfAbsent(counterName, counter);

        if (oldCounter == null) {
            // this is the 1st time
            DefaultMonitorRegistry.getInstance().register(counter);
        } else {
            // someone beat us to it, take their obj instead
            counter = oldCounter;
        }

        long increment = val - counter.getValue().longValue();
        ((Counter) counter).increment(increment);

    }

    @Override
    public String getName() {
        return TaskName;
    }

    /**
     * Returns a timer that enables this task to run once every 60 seconds
     * 
     * @return TaskTimer
     */
    public static TaskTimer getTimer() {
        // run once every 30 seconds
        return new SimpleTimer(TaskName, 30 * 1000);
    }

}
