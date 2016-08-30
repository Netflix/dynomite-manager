/**
 * Copyright 2016 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.defaultimpl;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.utils.ProcessTuner;
import com.netflix.dynomitemanager.IInstanceState;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration;
import com.netflix.dynomitemanager.defaultimpl.FloridaStandardTuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.DYNO_REDIS_CONF_PATH;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;


@Singleton
public class FloridaStandardTuner implements ProcessTuner {
	private static final Logger logger = LoggerFactory.getLogger(FloridaStandardTuner.class);
	private static final String ROOT_NAME = "dyn_o_mite";
	private static final String PROC_MEMINFO_PATH = "/proc/meminfo";
	public static final long GB_2_IN_KB = 2L * 1024L * 1024L;
	public static final String REDIS_CONF_MAXMEMORY_PATTERN = "^maxmemory\\s*[0-9][0-9]*[a-zA-Z]*";
	public static final String REDIS_CONF_APPENDONLY = "^appendonly\\s*[a-zA-Z]*";
	public static final String REDIS_CONF_APPENDFSYNC = "^ appendfsync\\s*[a-zA-Z]*";
	public static final String REDIS_CONF_AUTOAOFREWRITEPERCENTAGE = "^auto-aof-rewrite-percentage\\s*[0-9][0-9]*[a-zA-Z]*";
	public static final String REDIS_CONF_STOP_WRITES_BGSAVE_ERROR = "^stop-writes-on-bgsave-error\\s*[a-zA-Z]*";
	public static final String REDIS_CONF_SAVE_SCHEDULE = "^#\\ssave\\s[0-9]*\\s[0-9]*";

	protected final IConfiguration config;
	protected final InstanceIdentity ii;
	protected final IInstanceState instanceState;

	public static final Pattern MEMINFO_PATTERN = Pattern.compile("MemTotal:\\s*([0-9]*)");

	@Inject
	public FloridaStandardTuner(IConfiguration config, InstanceIdentity ii, IInstanceState instanceState) {
		this.config = config;
		this.ii = ii;
		this.instanceState = instanceState;
	}

	@SuppressWarnings("unchecked")
	public void writeAllProperties(String yamlLocation, String hostname, String seedProvider) throws IOException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		File yamlFile = new File(yamlLocation);
		Map map = (Map) yaml.load(new FileInputStream(yamlFile));
		Map entries = (Map) map.get(ROOT_NAME);

		entries.put("auto_eject_hosts", config.getAutoEjectHosts());
		entries.put("rack", config.getRack());
		entries.put("distribution", config.getDistribution());
		entries.put("dyn_listen", config.getDynListenPort());
		entries.put("dyn_seed_provider", config.getSeedProviderName());
		entries.put("gos_interval", config.getGossipInterval());
		entries.put("hash", config.getHash());
		entries.put("listen", config.getClientListenPort());
		entries.put("preconnect", config.getPreconnect());
		entries.put("server_retry_timeout", config.getServerRetryTimeout());
		entries.put("timeout", config.getTimeout());
		entries.put("tokens", ii.getTokens());
		entries.put("secure_server_option", config.getSecuredOption());
		entries.remove("redis");
		entries.put("datacenter", config.getRegion());
		entries.put("read_consistency", config.getReadConsistency());
		entries.put("write_consistency", config.getWriteConsistency());
		entries.put("pem_key_file", "/apps/dynomite/conf/dynomite.pem");

		List seedp = (List) entries.get("dyn_seeds");
		if (seedp == null) {
			seedp = new ArrayList<String>();
			entries.put("dyn_seeds", seedp);
		} else {
			seedp.clear();
		}

		List<String> seeds = ii.getSeeds();
		if (seeds.size() != 0) {
			for (String seed : seeds) {
				seedp.add(seed);
			}
		} else {
			entries.remove("dyn_seeds");
		}

		List servers = (List) entries.get("servers");
		if (servers == null) {
			servers = new ArrayList<String>();
			entries.put("servers", servers);
		} else {
			servers.clear();
		}

		if (config.getClusterType() == DynomitemanagerConfiguration.DYNO_REDIS) {
			entries.put("data_store", 0);
			servers.add("127.0.0.1:22122:1");
		} else {
			entries.put("data_store", 1);
			servers.add("127.0.0.1:11211:1");
		}

		if (!this.instanceState.getYmlWritten()) {
			logger.info("YAML Dump: ");
			logger.info(yaml.dump(map));
			if (config.getClusterType() == DynomitemanagerConfiguration.DYNO_REDIS) {
				updateRedisConfiguration();
			}
		} else {
			logger.info("Updating dynomite.yml with latest information");
		}
		yaml.dump(map, new FileWriter(yamlLocation));

		this.instanceState.setYmlWritten(true);
	}


	@SuppressWarnings("unchecked")
	public void updateAutoBootstrap(String yamlFile, boolean autobootstrap) throws IOException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);
		@SuppressWarnings("rawtypes")
		Map map = (Map) yaml.load(new FileInputStream(yamlFile));
		//Dont bootstrap in restore mode
		map.put("auto_bootstrap", autobootstrap);
		logger.info("Updating yaml" + yaml.dump(map));
		yaml.dump(map, new FileWriter(yamlFile));
	}


	private void updateRedisConfiguration() throws IOException {
		long storeMaxMem = getStoreMaxMem();

		// Updating the file.
		logger.info("Updating Redis conf: " + DYNO_REDIS_CONF_PATH);
		Path confPath = Paths.get(DYNO_REDIS_CONF_PATH);
		Path backupPath = Paths.get(DYNO_REDIS_CONF_PATH + ".bkp");

		// backup the original baked in conf only and not subsequent updates
		if (!Files.exists(backupPath)) {
			logger.info("Backing up baked in Redis config at: " + backupPath);
			Files.copy(confPath, backupPath, COPY_ATTRIBUTES);
		}


		if (config.isPersistenceEnabled() && config.isAof()) {
			logger.info("Persistence with AOF is enabled");
		} else if (config.isPersistenceEnabled() && !config.isAof()) {
			logger.info("Persistence with RDB is enabled");
		}

		// Not using Properties file to load as we want to retain all comments,
		// and for easy diffing with the ami baked version of the conf file.
		List<String> lines = Files.readAllLines(confPath, Charsets.UTF_8);
		boolean saveReplaced = false;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("#") && !line.matches(REDIS_CONF_SAVE_SCHEDULE)) {
				continue;
			}
			if (line.matches(REDIS_CONF_MAXMEMORY_PATTERN)) {
				String maxMemConf = "maxmemory " + storeMaxMem + "kb";
				logger.info("Updating Redis property: " + maxMemConf);
				lines.set(i, maxMemConf);
			}
			// Persistence configuration
			if (config.isPersistenceEnabled() && config.isAof()) {
				if (line.matches(REDIS_CONF_APPENDONLY)) {
					String appendOnly = "appendonly yes";
					logger.info("Updating Redis property: " + appendOnly);
					lines.set(i, appendOnly);
				} else if (line.matches(REDIS_CONF_APPENDFSYNC)) {
					String appendfsync = "appendfsync no";
					logger.info("Updating Redis property: " + appendfsync);
					lines.set(i, appendfsync);
				} else if (line.matches(REDIS_CONF_AUTOAOFREWRITEPERCENTAGE)) {
					String autoAofRewritePercentage = "auto-aof-rewrite-percentage 100";
					logger.info("Updating Redis property: " + autoAofRewritePercentage);
					lines.set(i, autoAofRewritePercentage);
				} else if (line.matches(REDIS_CONF_SAVE_SCHEDULE)) {
					String saveSchedule = "# save 60 10000"; // if we select AOF, it is better to stop RDB
					logger.info("Updating Redis property: " + saveSchedule);
					lines.set(i, saveSchedule);
				}
			} else if (config.isPersistenceEnabled() && !config.isAof()) {

				if (line.matches(REDIS_CONF_STOP_WRITES_BGSAVE_ERROR)) {
					String bgsaveerror = "stop-writes-on-bgsave-error no";
					logger.info("Updating Redis property: " + bgsaveerror);
					lines.set(i, bgsaveerror);
				} else if (line.matches(REDIS_CONF_SAVE_SCHEDULE) && !saveReplaced) {
					saveReplaced = true;
					String saveSchedule = "save 60 10000"; //after 60 sec if at least 10000 keys changed
					logger.info("Updating Redis property: " + saveSchedule);
					lines.set(i, saveSchedule);
				} else if (line.matches(REDIS_CONF_APPENDONLY)) { // if we select RDB, it is better to stop AOF
					String appendOnly = "appendonly no";
					logger.info("Updating Redis property: " + appendOnly);
					lines.set(i, appendOnly);
				}
			}
		}

		Files.write(confPath, lines, Charsets.UTF_8, WRITE, TRUNCATE_EXISTING);
	}

	private long getStoreMaxMem() {
		int memPct = config.getStorageMemPercent();
		// Long is big enough for the amount of ram is all practical systems that we deal with.
		long totalMem = getTotalAvailableSystemMemory();
		long storeMaxMem = (totalMem * memPct) / 100;
		storeMaxMem = ((totalMem - storeMaxMem) > GB_2_IN_KB) ? storeMaxMem : (totalMem - GB_2_IN_KB);

		logger.info(String.format("totalMem:%s Setting %s storage max mem to %s", totalMem,
				config.getClusterType() == 1 ? "Redis" : "Memcache", storeMaxMem));
		return storeMaxMem;
	}

	// Returns the number of kb.
	public long getTotalAvailableSystemMemory() {
		String memInfo;
		try {
			memInfo = new Scanner(new File(PROC_MEMINFO_PATH)).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			String errMsg = String.format("Unable to find %s file for retrieving memory info.", PROC_MEMINFO_PATH);
			logger.error(errMsg);
			throw new RuntimeException(errMsg);
		}

		Matcher matcher = MEMINFO_PATTERN.matcher(memInfo);
		if (matcher.find()) {
			try {
				return Long.parseLong(matcher.group(1));
			} catch (NumberFormatException e) {
				logger.info("Failed to parse long", e);
			}
		}

		String errMsg = String.format("Could not extract total mem using pattern %s from:\n%s ", MEMINFO_PATTERN, memInfo);
		logger.error(errMsg);
		throw new RuntimeException(errMsg);
	}
}
