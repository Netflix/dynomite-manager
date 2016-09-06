/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.monitoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.*;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple class that encapsulates a {@link Task} implementation that fetches metrics from a remote source in the form
 * of a json payload and then translates the payload into a set of Servo metrics - {@link Counter} and {@link Gauge}
 *
 * Few things to note.
 *
 *   1. The remote resource is specified via a url. There is a default path. Override this path using archaius (if needed)
 *      The fast property name is  'dynomitemanager.metrics.url'
 *
 *   2. The json response can have nested maps of metrics. This class traverses all the levels and flattens the metrics out
 *      using prefixs   e.g     {a  {b {c} } }   will get flattened out to a__b__c as a metric name.
 *
 *   3. Most metrics are {@link Counter}s, but some metrics can be {@link Gauge}s. For {@link Gauge}s one must configure the
 *      active whitelist of metric names via archaius.
 *      The fast property name is  'dynomitemanager.metrics.gauge.whitelist'
 *
 *   4. Note that this class maintains a map of servo counters and gauges, so that they don't have to be recreated all the time.
 *      Hence this class maintains state and needs to be a singleton.
 *
 *
 * @author poberai
 *
 */
@Singleton public class ServoMetricsTask extends Task {

		private static final Logger Logger = LoggerFactory.getLogger(ServoMetricsTask.class);

		// The Task name for identification
		public static final String TaskName = "Servo-Metrics-Task";

		// Fast Property for configuring the remote resource to talk to
		private final DynamicStringProperty ServerMetricsUrl = DynamicPropertyFactory.getInstance()
				.getStringProperty("dynomitemanager.metrics.url", "http://localhost:22222/info");

		// Fast Property for configuring a gauge whitelist (if needed)
		private final DynamicStringProperty GaugeWhitelist = DynamicPropertyFactory.getInstance()
				.getStringProperty("dynomitemanager.metrics.gauge.whitelist", "");

		// The gauge whitelist that is being maintained. Note that we keep a reference to it that can be update dynamically
		// if the fast property is changed externally
		private final AtomicReference<Set<String>> gaugeFilter = new AtomicReference<Set<String>>(
				new HashSet<String>());

		// The map of servo metrics
		private final ConcurrentHashMap<String, NumericMonitor<Number>> metricMap = new ConcurrentHashMap<String, NumericMonitor<Number>>();

		private final InstanceState state;

		/**
		 * Default constructor
		 * @param config
		 */
		@Inject public ServoMetricsTask(IConfiguration config, InstanceState state) {

				super(config);
				this.state = state;

				initGaugeWhitelist();

				GaugeWhitelist.addCallback(new Runnable() {

						@Override public void run() {
								initGaugeWhitelist();
						}
				});
		}

		/**
		 * Returns a timer that enables this task to run once every 60 seconds
		 * @return TaskTimer
		 */
		public static TaskTimer getTimer() {
				// run once every 30 seconds
				return new SimpleTimer(TaskName, 15 * 1000);
		}

		/**
		 * The name of the task
		 * @return String
		 */
		@Override public String getName() {
				return TaskName;
		}

		/**
		 * @return metricMap
		 */
		public ConcurrentHashMap<String, NumericMonitor<Number>> getMetricsMap() {
				return metricMap;
		}

		/**
		 *  Main execute() impl for this task. It makes a call to the remote service, and if the response is a 200
		 *  with a json body, then this parses the json response into servo metrics.
		 *
		 *  Note that new metrics that weren't tracked before start being tracked, and old metrics that are already
		 *  there are simply updated with the new values from the http response.
		 *
		 */
		@Override public void execute() throws Exception {

				HttpClient client = null;
				GetMethod get = null;

				//update health state.  I think we can merge the health check and info check into one check later.
				//However, health check also touches the underneath storage, not just Dynomite
				processGaugeMetric("dynomite__health", state.isHealthy() ? 1L : 0L);

				try {
						client = new HttpClient();
						client.getHttpConnectionManager().getParams().setConnectionTimeout(2000);
						get = new GetMethod(ServerMetricsUrl.get());

						int statusCode = client.executeMethod(get);
						if (!(statusCode == 200)) {
								Logger.error("Got non 200 status code from " + ServerMetricsUrl.get());
								return;
						}

						String response = get.getResponseBodyAsString();
						if (Logger.isDebugEnabled()) {
								Logger.debug("Received response from " + ServerMetricsUrl.get() + "\n" + response);
						}

						if (!response.isEmpty()) {
								processJsonResponse(response);
						} else {
								Logger.error("Cannot parse empty response from " + ServerMetricsUrl.get());
						}

				} catch (Exception e) {
						Logger.error("Failed to get metrics from url: " + ServerMetricsUrl.get(), e);
						e.printStackTrace();
				} catch (Throwable t) {
						Logger.error("FAILED to get metrics from url: " + ServerMetricsUrl.get(), t);
						t.printStackTrace();
				} finally {
						if (get != null) {
								get.releaseConnection();
						}
						if (client != null) {
								client.getHttpConnectionManager().closeIdleConnections(10);
						}

				}
		}

		/**
		 * Parse the Json Payload and convert them to metrics.
		 *
		 * Example Json Payload
		 *
		 *  {"service":"dynomite", "source":"dynomitemanager-i-16ca1846", "version":"0.3.1", "uptime":40439, "timestamp":1399064677, "datacenter":"DC1",
		 *  "dyn_o_mite":
		 *           {"client_eof":0, "client_err":0, "client_connections":0, "server_ejects":0, "forward_error":0, "fragments":0, "stats_count":0,
		 *           "guage" :
		 *           {
		 *           }
		 *           "127.0.0.1":
		 *                     {"server_eof":0, "server_err":0, "server_timedout":0, "server_connections":0, "server_ejected_at":0, "requests":0,
		 *                     "request_bytes":0, "responses":0, "response_bytes":0, "in_queue":0, "in_queue_bytes":0, "out_queue":0,
		 *                     "out_queue_bytes":0
		 *                     }
		 *           }
		 *  }
		 *
		 * @param json
		 * @throws Exception
		 */
		public void processJsonResponse(String json) throws Exception {

				JSONParser parser = new JSONParser();
				JSONObject obj = (JSONObject) parser.parse(json);

				String service = (String) obj.get("service");

				if (service.isEmpty()) {
						Logger.error("Missing required key 'service' in json response: " + json);
						return;
				}

				//uptime
				Long uptime = (Long) obj.get("uptime");
				if (uptime == null) {
						Logger.error("Missing required key 'uptime' in json response: " + json);
						uptime = 0L;
				}
				processCounterMetric(service + "__uptime", uptime);

				String[] fields = { "latency_max", "latency_999th", "latency_99th", "latency_95th", "latency_mean",
						"payload_size_max", "payload_size_999th", "payload_size_99th", "payload_size_95th",
						"payload_size_mean", "alloc_msgs", "free_msgs", "average_cross_region_rtt", "payload_size_mean",
						"alloc_msgs", "free_msgs", "average_cross_region_rtt", "99_cross_region_rtt",
						"average_cross_zone_latency", "99_cross_zone_latency", "average_server_latency",
						"99_server_latency", "average_cross_region_queue_wait", "99_cross_region_queue_wait",
						"average_cross_zone_queue_wait", "99_cross_zone_queue_wait", "average_server_queue_wait",
						"99_server_queue_wait", "client_out_queue_99", "server_in_queue_99", "server_out_queue_99",
						"dnode_client_out_queue_99", "peer_in_queue_99", "peer_out_queue_99", "remote_peer_in_queue_99",
						"remote_peer_out_queue_99", "alloc_mbufs", "free_mbufs" };
				for (int i = 0; i < fields.length; i++) {
						Long val = (Long) obj.get(fields[i]);
						if (val == null) {
								//Logger.error("Missing required key " + fields[i] + " in json response: " + json);
								val = 0L;
						}
						processGaugeMetric(service + "__" + fields[i], val);
				}
				JSONObject stats = (JSONObject) obj.get("dyn_o_mite");
				if (stats == null) {
						Logger.error("Missing key 'dyn_o_mite' in json response: " + json);
						return;
				}
				parseObjectMetrics(service, stats);
		}

		/**
		 * Helper to recursively flatten out a metric from a nested collection
		 * @param namePrefix
		 * @param obj
		 */
		private void parseObjectMetrics(String namePrefix, JSONObject obj) {

				for (Object key : obj.keySet()) {

						Object val = obj.get(key);

						if (val instanceof JSONObject) {
								parseObjectMetrics(namePrefix + "__" + key, (JSONObject) val);
						} else {
								if (gaugeFilter.get().contains((String) key)) {
										processGaugeMetric(namePrefix + "__" + (String) key, (Long) val);
								} else {
										processCounterMetric(namePrefix + "__" + (String) key, (Long) val);
								}
						}
				}

		}

		/**
		 * Helper that tracks the metric value in a {@link Counter} A new one is created if it does not exist.
		 * @param counterName
		 * @param val
		 */
		private void processCounterMetric(String counterName, Long val) {

				if (Logger.isDebugEnabled()) {
						Logger.debug("Process counter: " + counterName + " " + val);
				}

				NumericMonitor<Number> counter = metricMap.get(counterName);
				if (counter != null) {
						long increment = val - counter.getValue().longValue();
						((Counter) counter).increment(increment);
						return;
				}

				counter = Monitors.newCounter(counterName);
				NumericMonitor<Number> oldCounter = metricMap.putIfAbsent(counterName, counter);

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

		/**
		 * Helper that tracks the metric value in a {@link Gauge} A new one is created if it does not exist.
		 *
		 * @param gaugeName
		 * @param val
		 */
		private void processGaugeMetric(String gaugeName, Long val) {

				if (Logger.isDebugEnabled()) {
						Logger.debug("Process guage: " + gaugeName + " " + val);
				}

				NumericMonitor<Number> gauge = metricMap.get(gaugeName);
				if (gauge != null) {
						((SimpleGauge) gauge).setValue(val);
						return;
				}

				gauge = new SimpleGauge(gaugeName, val);
				NumericMonitor<Number> oldGauge = metricMap.putIfAbsent(gaugeName, gauge);

				if (oldGauge == null) {
						((SimpleGauge) gauge).setValue(val);
						DefaultMonitorRegistry.getInstance().register(gauge);
				} else {
						((SimpleGauge) oldGauge).setValue(val);
				}
		}

		/**
		 * Helper that tracks the gauge whitelist using defaults and what is set in the fast property
		 */
		@SuppressWarnings("unchecked") private void initGaugeWhitelist() {

				Set<String> set = new HashSet<String>();
				set.add("server_connections");
				set.add("client_connections");
				set.add("dnode_client_connections");
				set.add("peer_connections");
				//set.add("client_dropped_requests");
				//set.add("alloc_msgs");

				String s = GaugeWhitelist.get();
				if (!s.isEmpty()) {

						String[] parts = s.split(",");
						if (parts != null && parts.length > 0) {
								set.addAll(Arrays.asList(parts));
						}
				}
				gaugeFilter.set(set);
		}

		/**
		 * Simple impl of the {@link Gauge}
		 * Note that it maintains a threadsafe reference to the actual value being monitored
		 *
		 * @author poberai
		 *
		 */
		private class SimpleGauge implements Gauge<Number> {

				private final MonitorConfig mConfig;
				private final AtomicReference<Number> value = new AtomicReference<Number>(null);

				private SimpleGauge(String name, Number number) {
						mConfig = MonitorConfig.builder(name).build();
						value.set(number);
				}

				@Override public Number getValue() {
						return value.get();
				}

				public Number getValue(int pollerIndex) {
						return getValue();
				}

				@Override public MonitorConfig getConfig() {
						return mConfig;
				}

				public void setValue(Number n) {
						value.set(n);
				}
		}
}
