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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

public class RedisInfoParser {

	private static final Set<String> WHITE_LIST = new HashSet<String>();

	static {
		WHITE_LIST.add("uptime_in_seconds");
		WHITE_LIST.add("connected_clients");
		WHITE_LIST.add("client_longest_output_list");
		WHITE_LIST.add("client_biggest_input_buf");
		WHITE_LIST.add("blocked_clients");
		WHITE_LIST.add("used_memory");
		WHITE_LIST.add("used_memory_rss");
		WHITE_LIST.add("used_memory_lua");
		WHITE_LIST.add("mem_fragmentation_ratio");
		WHITE_LIST.add("rdb_changes_since_last_save");
		WHITE_LIST.add("rdb_last_save_time");
		WHITE_LIST.add("aof_enabled");
		WHITE_LIST.add("aof_rewrite_in_progress");
		WHITE_LIST.add("total_connections_received");
		WHITE_LIST.add("total_commands_processed");
		WHITE_LIST.add("instantaneous_ops_per_sec");
		WHITE_LIST.add("rejected_connections");
		WHITE_LIST.add("expired_keys");
		WHITE_LIST.add("evicted_keys");
		WHITE_LIST.add("keyspace_hits");
		WHITE_LIST.add("keyspace_misses");
		WHITE_LIST.add("used_cpu_sys");
		WHITE_LIST.add("used_cpu_user");
		WHITE_LIST.add("db0");
	}

	public RedisInfoParser() {

	}

	public Map<String, Long> parse(Reader inReader) throws Exception {

		final Map<String, Long> metrics = new HashMap<String, Long>();
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(inReader);

			List<StatsSection> sections = new ArrayList<StatsSection>();

			boolean stop = false;
			while (!stop) {
				StatsSection section = new StatsSection(reader, RuleIter);
				section.initSection();

				if (section.isEmpty()) {
					stop = true;
					break;
				}

				section.parseSectionData();

				if (section.data.isEmpty()) {
					continue;
				}

				sections.add(section);
			}

			for (StatsSection section : sections) {
				metrics.putAll(section.getMetrics());
			}

		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		return metrics;
	}

	private class StatsSection {

		private final BufferedReader reader;

		private String sectionName;
		private String sectionNamePrefix = "Redis_";

		private final Map<String, Long> data = new HashMap<String, Long>();
		private final SectionRule sectionRule;

		private StatsSection(BufferedReader br, SectionRule rule) {
			reader = br;
			sectionRule = rule;
		}

		private boolean isEmpty() {
			return sectionName == null && data.isEmpty();
		}

		private void initSection() throws Exception {

			String line = null;

			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					continue;
				} else {
					break;
				}
			}

			if (line == null) {
				return;
			}

			sectionName = readSectionName(line);
			if (sectionName != null && !sectionName.isEmpty()) {
				sectionNamePrefix = "Redis_" + sectionName + "_";
			}
		}

		private void parseSectionData() throws Exception {

			String line = reader.readLine();

			while (line != null && !line.isEmpty()) {
				processLine(line.trim());
				line = reader.readLine();
			}
		}

		private String readSectionName(String line) {
			String[] parts = line.split(" ");
			if (parts.length != 2) {
				return null;
			}
			return parts[1];
		}

		private void processLine(String line) throws Exception {

			String[] parts = line.split(":");
			if (parts.length != 2) {
				return;
			}
			String name = parts[0];
			String sVal = parts[1];

			//while list filtering
			if (!WHITE_LIST.contains(name))
				return;

			if (sVal.endsWith("M")) {
				sVal = sVal.substring(0, sVal.length() - 1);
			}

			if (sectionRule.processSection(this, name, sVal)) {
				return; // rule already applied. data is processed with custom logic
			}

			// else do generic rule processing
			Double val = null;
			try {
				val = Double.parseDouble(sVal);
			} catch (NumberFormatException nfe) {
				val = null;
			}

			if (val != null) {
				data.put(name, val.longValue());
			}
		}

		private Map<String, Long> getMetrics() {

			Map<String, Long> map = new HashMap<String, Long>();
			for (String key : data.keySet()) {
				map.put(sectionNamePrefix + key, data.get(key));
			}
			return map;
		}

	}

	private interface SectionRule {
		boolean processSection(StatsSection section, String key, String value);
	}

	private SectionRule Rule0 = new SectionRule() {

		@Override
		public boolean processSection(StatsSection section, String key, String value) {

			if (section.sectionName.equals("Server")) {
				if (key.equals("uptime_in_seconds")) {
					try {
						Double dVal = Double.parseDouble(value);
						section.data.put(key, dVal.longValue());
						return true;
					} catch (NumberFormatException e) {
					}
				}
			}
			return false;
		}

	};

	private SectionRule Rule1 = new SectionRule() {

		@Override
		public boolean processSection(StatsSection section, String key, String value) {

			if (section.sectionName.equals("Memory")) {
				if (key.equals("mem_fragmentation_ratio")) {
					try {
						Double dVal = Double.parseDouble(value);
						dVal = dVal * 100;
						section.data.put(key, dVal.longValue());
						return true;
					} catch (NumberFormatException e) {
					}
				}
			}
			return false;
		}

	};

	private SectionRule Rule2 = new SectionRule() {

		@Override
		public boolean processSection(StatsSection section, String key, String value) {

			if (section.sectionName.equals("Persistence")) {
				if (key.equals("rdb_last_bgsave_status") || key.equals("aof_last_bgrewrite_status")
						|| key.equals("aof_last_write_status")) {
					Long val = value.equalsIgnoreCase("ok") ? 1L : 0L;
					section.data.put(key, val);
					return true;
				}
			}
			return false;
		}

	};

	private SectionRule Rule3 = new SectionRule() {

		@Override
		public boolean processSection(StatsSection section, String key, String value) {

			if (section.sectionName.equals("Keyspace")) {
				if (key.equals("db0")) {
					String[] parts = value.split(",");
					for (String part : parts) {
						addPart(key, part, section);
					}
					return true;
				}
			}
			return false;
		}

		private void addPart(String parentKey, String keyVal, StatsSection section) {
			String[] parts = keyVal.split("=");
			if (parts.length != 2) {
				return;
			}
			try {
				String key = parentKey + "_" + parts[0];
				Double dVal = Double.parseDouble(parts[1]);
				section.data.put(key, dVal.longValue());
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	};

	private SectionRule RuleIter = new SectionRule() {

		SectionRule[] arr = { Rule0, Rule1, Rule2, Rule3 };
		final List<SectionRule> rules = Arrays.asList(arr);

		@Override
		public boolean processSection(StatsSection section, String key, String value) {

			for (SectionRule rule : rules) {
				if (rule.processSection(section, key, value)) {
					return true;
				}
			}
			return false;
		}
	};

	public static void main(String[] args) throws FileNotFoundException, Exception {

		File file = new File("./src/test/resources/redis_info.txt");

		RedisInfoParser parser = new RedisInfoParser();
		Map<String, Long> metrics = parser.parse(new FileReader(file));

		Iterator iter = metrics.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			Long value = metrics.get(key);
			System.out.println("Key: " + key + ", value: " + value);
		}

		Assert.assertTrue(metrics.get("Redis_Server_uptime_in_seconds") == 1234L);
		Assert.assertTrue(metrics.get("Redis_Clients_connected_clients") == 4L);
		Assert.assertTrue(metrics.get("Redis_Clients_client_longest_output_list") == 0L);
		Assert.assertTrue(metrics.get("Redis_Clients_client_biggest_input_buf") == 0L);
		Assert.assertTrue(metrics.get("Redis_Clients_blocked_clients") == 0L);
		Assert.assertTrue(metrics.get("Redis_Memory_used_memory") == 314569968L);
		//Assert.assertTrue(metrics.get("Redis_Memory_used_memory_human") == 300L);
		//Assert.assertTrue(metrics.get("Redis_Memory_used_memory_rss") == 328806400L);
		//Assert.assertTrue(metrics.get("Redis_Memory_used_memory_peak") == 314569968L);
		//Assert.assertTrue(metrics.get("Redis_Memory_used_memory_peak_human") == 300L);
		//Assert.assertTrue(metrics.get("Redis_Memory_used_memory_lua") == 33792L);
		Assert.assertTrue(metrics.get("Redis_Memory_mem_fragmentation_ratio") == 105L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_loading") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_rdb_changes_since_last_save") == 53046299L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_rdb_bgsave_in_progress") == 0L);
		Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_save_time") == 1411544331L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_bgsave_status") == 1L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_rdb_last_bgsave_time_sec") == -1L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_rdb_current_bgsave_time_sec") == -1L);
		Assert.assertTrue(metrics.get("Redis_Persistence_aof_enabled") == 0L);
		Assert.assertTrue(metrics.get("Redis_Persistence_aof_rewrite_in_progress") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_aof_rewrite_scheduled") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_rewrite_time_sec") == -1L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_aof_current_rewrite_time_sec") == -1L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_bgrewrite_status") == 1L);
		//Assert.assertTrue(metrics.get("Redis_Persistence_aof_last_write_status") == 1L);
		Assert.assertTrue(metrics.get("Redis_Stats_total_connections_received") == 3995L);
		Assert.assertTrue(metrics.get("Redis_Stats_total_commands_processed") == 94308679L);
		Assert.assertTrue(metrics.get("Redis_Stats_instantaneous_ops_per_sec") == 6321L);
		Assert.assertTrue(metrics.get("Redis_Stats_rejected_connections") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Stats_sync_full") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Stats_sync_partial_ok") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Stats_sync_partial_err") == 0L);
		Assert.assertTrue(metrics.get("Redis_Stats_expired_keys") == 0L);
		Assert.assertTrue(metrics.get("Redis_Stats_evicted_keys") == 0L);
		Assert.assertTrue(metrics.get("Redis_Stats_keyspace_hits") == 41254397L);
		Assert.assertTrue(metrics.get("Redis_Stats_keyspace_misses") == 0L);
		Assert.assertTrue(metrics.get("Redis_percent_hits") == 100L);
		//Assert.assertTrue(metrics.get("Redis_Stats_pubsub_channels") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Stats_pubsub_patterns") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Stats_latest_fork_usec") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Replication_connected_slaves") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Replication_master_repl_offset") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_active") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_size") == 1048576L);
		//Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_first_byte_offset") == 0L);
		//Assert.assertTrue(metrics.get("Redis_Replication_repl_backlog_histlen") == 0L);
		Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_sys") == 2052L);
		Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_user") == 793L);
		//Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_sys_children") == 0L);
		//Assert.assertTrue(metrics.get("Redis_CPU_used_cpu_user_children") == 0L);
		Assert.assertTrue(metrics.get("Redis_Keyspace_db0_keys") == 2499968L);
		Assert.assertTrue(metrics.get("Redis_Keyspace_db0_expires") == 0L);
		Assert.assertTrue(metrics.get("Redis_Keyspace_db0_avg_ttl") == 0L);
	}
}
