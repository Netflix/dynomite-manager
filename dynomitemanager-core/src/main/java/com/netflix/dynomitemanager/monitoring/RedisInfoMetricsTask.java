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
import com.netflix.dynomitemanager.storage.RedisInfoParser;
import com.netflix.dynomitemanager.storage.StorageProxy;
import com.netflix.nfsidecar.scheduler.SimpleTimer;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.scheduler.TaskTimer;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.NumericMonitor;

@Singleton
public class RedisInfoMetricsTask extends Task {

    private static final Logger Logger = LoggerFactory.getLogger(RedisInfoMetricsTask.class);
    public static final String TaskName = "Redis-Info-Task";

    private static final Set<String> COUNTER_LIST = new HashSet<String>();

    static {
        COUNTER_LIST.add("Redis_Stats_instantaneous_ops_per_sec");
    }

    private final ConcurrentHashMap<String, LongGauge> redisInfoGaugeMetrics = new ConcurrentHashMap<String, LongGauge>();
    private final ConcurrentHashMap<String, NumericMonitor<Number>> redisInfoCounterMap = new ConcurrentHashMap<String, NumericMonitor<Number>>();

    private JedisFactory jedisFactory;
    private StorageProxy storageProxy;

    /**
     * Default constructor
     * 
     * @param storageProxy
     * @param jedisFactory
     */
    @Inject
    public RedisInfoMetricsTask(StorageProxy storageProxy, JedisFactory jedisFactory) {
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

    public static TaskTimer getTimer() {
        // run once every 30 seconds
        return new SimpleTimer(TaskName, 30 * 1000);
    }

}
