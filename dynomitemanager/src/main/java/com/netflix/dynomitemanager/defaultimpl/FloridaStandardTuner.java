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
package com.netflix.dynomitemanager.defaultimpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.dynomite.DynomiteConfiguration;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.ProcessTuner;
import com.netflix.dynomitemanager.IInstanceState;
import com.netflix.dynomitemanager.defaultimpl.FloridaStandardTuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Generate and write the dynomite.yaml and redis.conf configuration files to disk.
 */
@Singleton
public class FloridaStandardTuner implements ProcessTuner {

    private static final Logger logger = LoggerFactory.getLogger(FloridaStandardTuner.class);
    private static final String ROOT_NAME = "dyn_o_mite";

    DynomiteConfiguration dynomiteConfig;
    protected final IConfiguration config;
    protected final InstanceIdentity ii;
    protected final IInstanceState instanceState;
    protected final IStorageProxy storageProxy;

    @Inject
    public FloridaStandardTuner(IConfiguration config, DynomiteConfiguration dynomiteConfig, InstanceIdentity ii,
            IInstanceState instanceState, IStorageProxy storageProxy) {
	this.config = config;
        this.dynomiteConfig = dynomiteConfig;
	this.ii = ii;
	this.instanceState = instanceState;
	this.storageProxy = storageProxy;
    }

    /**
     * Generate dynomite.yaml.
     *
     * @param yamlLocation path to the dynomite.yaml file
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void writeAllProperties(String yamlLocation) throws IOException {
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
	entries.put("dyn_seed_provider", dynomiteConfig.getSeedProvider());
	entries.put("gos_interval", config.getGossipInterval());
	entries.put("hash", config.getHash());
	entries.put("listen", dynomiteConfig.getClientListen());
	entries.put("preconnect", config.getPreconnect());
	entries.put("server_retry_timeout", config.getServerRetryTimeout());
	entries.put("timeout", config.getTimeout());
	entries.put("tokens", ii.getTokens());
	entries.put("secure_server_option", config.getSecuredOption());
	entries.remove("redis");
	entries.put("datacenter", config.getDataCenter());
	entries.put("read_consistency", dynomiteConfig.getReadConsistency());
	entries.put("write_consistency", dynomiteConfig.getWriteConsistency());
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

	entries.put("data_store", storageProxy.getEngineNumber());
	servers.add(storageProxy.getIpAddress() + ":" + storageProxy.getPort()  +":" + 1);


	if (!this.instanceState.getYmlWritten()) {
	    logger.info("YAML Dump: ");
	    logger.info(yaml.dump(map));
	    storageProxy.updateConfiguration();
	} else {
	    logger.info("Updating dynomite.yml with latest information");
	}
	yaml.dump(map, new FileWriter(yamlLocation));

	this.instanceState.setYmlWritten(true);
    }

    /**
     * UNUSED METHOD
     *
     * @param yamlFile
     * @param autobootstrap
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void updateAutoBootstrap(String yamlFile, boolean autobootstrap) throws IOException {
	DumperOptions options = new DumperOptions();
	options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
	Yaml yaml = new Yaml(options);
	@SuppressWarnings("rawtypes")
	Map map = (Map) yaml.load(new FileInputStream(yamlFile));
	// Do not bootstrap in restore mode
	map.put("auto_bootstrap", autobootstrap);
	logger.info("Updating yaml" + yaml.dump(map));
	yaml.dump(map, new FileWriter(yamlFile));
    }

}
