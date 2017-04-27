package com.netflix.dynomitemanager.dynomite;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.storage.StorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.ProcessTuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Singleton
public class DynomiteStandardTuner implements ProcessTuner {
    private static final Logger logger = LoggerFactory.getLogger(DynomiteStandardTuner.class);
    private static final String ROOT_NAME = "dyn_o_mite";
    public static final long GB_2_IN_KB = 2L * 1024L * 1024L;

    protected final IConfiguration config;
    protected final InstanceIdentity ii;
    protected final StorageProxy storageProxy;
    protected final InstanceState instanceState;

    public static final Pattern MEMINFO_PATTERN = Pattern.compile("MemTotal:\\s*([0-9]*)");

    @Inject
    public DynomiteStandardTuner(IConfiguration config, InstanceIdentity ii, InstanceState instanceState,
            StorageProxy storageProxy) {
        this.config = config;
        this.ii = ii;
        this.instanceState = instanceState;
        this.storageProxy = storageProxy;

    }

    /**
     * we want to throw the exception for higher layer to handle it.
     */
    public void writeAllProperties(String yamlLocation) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        File yamlFile = new File(yamlLocation);
        Map map = (Map) yaml.load(new FileInputStream(yamlFile));
        Map<String, Object> entries = (Map) map.get(ROOT_NAME);

        entries.put("auto_eject_hosts", config.getDynomiteAutoEjectHosts());
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
        entries.put("pem_key_file", config.getAppHome() + "/conf/dynomite.pem");

        List<String> seedp = (List) entries.get("dyn_seeds");
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

        List<String> servers = (List) entries.get("servers");
        if (servers == null) {
            servers = new ArrayList<String>();
            entries.put("servers", servers);
        } else {
            servers.clear();
        }

        entries.put("data_store", storageProxy.getEngineNumber());
        servers.add(storageProxy.getIpAddress() + ":" + storageProxy.getPort() + ":" + 1);

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

    @SuppressWarnings("unchecked")
    public void updateAutoBootstrap(String yamlFile, boolean autobootstrap) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        @SuppressWarnings("rawtypes")
        Map map = (Map) yaml.load(new FileInputStream(yamlFile));
        // Dont bootstrap in restore mode
        map.put("auto_bootstrap", autobootstrap);
        logger.info("Updating yaml" + yaml.dump(map));
        yaml.dump(map, new FileWriter(yamlFile));
    }

}
