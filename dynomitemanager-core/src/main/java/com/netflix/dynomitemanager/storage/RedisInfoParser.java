package com.netflix.dynomitemanager.storage;

import java.io.BufferedReader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
	
	/**
	 * The following apply only for ARDB/RocksDB"       
	 */
        WHITE_LIST.add("used_disk_space");
        WHITE_LIST.add("rocksdb_memtable_total"); 
        WHITE_LIST.add("rocksdb_memtable_unflushed");
    }

    /**
     * This is to create a constructor for the test cases.
     */
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

	    // while list filtering
	    if (!WHITE_LIST.contains(name))
		return;

	    if (sVal.endsWith("M")) {
		sVal = sVal.substring(0, sVal.length() - 1);
	    }

	    if (sectionRule.processSection(this, name, sVal)) {
		return; // rule already applied. data is processed with custom
			// logic
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
}
