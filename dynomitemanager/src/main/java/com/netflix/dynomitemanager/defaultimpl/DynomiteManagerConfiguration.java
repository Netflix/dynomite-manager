/**
# Copyright 2016 Netflix, Inc. and [Dynomite Manager contributors](https://github.com/Netflix/dynomite-manager/blob/dev/CONTRIBUTORS.md)
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

import java.util.List;

import com.netflix.config.DynamicBooleanProperty;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.dynomitemanager.sidecore.IConfigSource;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.utils.RetryableCallable;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;


/**
 * Define the list of available Dynomite Manager configuration options, then set options based on the environment and an
 * external configuration.
 *
 * Dynomite Manager properties may be provided via the following mechanisms:
 * <ul>
 * <li>Archaius: Excellent option for enterprise deployments as it provides dynamic properties (i.e. configuration
 * management)
 * <li>Environment variables: Localized configuration passed in via environment variables
 * <li>Java properties: Localized configuration passed in via the command line in an init scrip
 * </ul>
 */
@Singleton
public class DynomiteManagerConfiguration implements IConfiguration {
    public static final String DYNOMITEMANAGER_PRE = "dm";
    public static final String CASSANDRA_PREFIX = "cassandra";
    public static final String DYNOMITE_PREFIX = "dynomite";
    public static final String REDIS_PREFIX = "redis";
    public static final String ARDB_PREFIX = "ardb";
    public static final String ROCKSDB_PREFIX = "rocksdb";
    public static final String DATASTORE_PREFIX = "datastore"; // Storage engine (aka backend)

    public static final String DYNOMITE_PROPS = DYNOMITEMANAGER_PRE + "." + DYNOMITE_PREFIX;
    public static final String DATASTORE_PROPS = DYNOMITEMANAGER_PRE + "." + DATASTORE_PREFIX;
    public static final String REDIS_PROPS = DYNOMITEMANAGER_PRE + "." + REDIS_PREFIX;
    public static final String ARDB_PROPS = DYNOMITEMANAGER_PRE + "." + ARDB_PREFIX;
    public static final String ARDB_ROCKSDB_PROPS = ARDB_PROPS + "." + ROCKSDB_PREFIX;
    public static final String CASSANDRA_PROPS = DYNOMITEMANAGER_PRE + "." + CASSANDRA_PREFIX;

    // Archaius
    // ========

    static {
        System.setProperty("archaius.configurationSource.defaultFileName", "dynomitemanager.properties");
    }

    private boolean getBooleanProperty(String envVar, String key, boolean defaultValue) {
        String envVal = System.getenv(envVar);
        if (envVal != null && !"".equals(envVal) && ("true".equals(envVal) || "false".equals(envVal))) {
            try {
                return Boolean.parseBoolean(envVal);
            } catch (NumberFormatException e) {
                logger.info(envVar + " must be a boolean (true, false). Using value from Archaius.");
            }
        }

        DynamicBooleanProperty property = DynamicPropertyFactory.getInstance().getBooleanProperty(key, defaultValue);
        return property.get();
    }

    private String getStringProperty(String envVar, String key, String defaultValue) {
        String envVal = System.getenv(envVar);
        if (envVal != null && !"".equals(envVal)) {
            return envVal;
        }

        DynamicStringProperty property = DynamicPropertyFactory.getInstance().getStringProperty(key, defaultValue);
        return property.get();
    }

    private int getIntProperty(String envVar, String key, int defaultValue) {
        String envVal = System.getenv(envVar);
        if (envVal != null && !"".equals(envVal)) {
            try {
                return Integer.parseInt(envVal);
            } catch (NumberFormatException e) {
                logger.info(envVar + " must be an integer. Using value from Archaius.");
            }
        }

        DynamicIntProperty property = DynamicPropertyFactory.getInstance().getIntProperty(key, defaultValue);
        return property.get();
    }

	// Dynomite
	// ========

    public static final String LOCAL_ADDRESS = "127.0.0.1";

    private static final String CONFIG_DYNOMITE_INSTALL_DIR = DYNOMITE_PROPS + ".install.dir";
    private static final String CONFIG_DYNOMITE_START_SCRIPT = DYNOMITE_PROPS + ".start.script";
    private static final String CONFIG_DYNOMITE_STOP_SCRIPT = DYNOMITE_PROPS + ".stop.script";

    // Cluster name is saved as tokens.appId in Cassandra.
    // The cluster name is used as the default AWS Security Group name, if SG name is null.
    private static final String CONFIG_DYNOMITE_CLUSTER_NAME = DYNOMITE_PROPS + ".cluster.name";
    private static final String CONFIG_DYNOMITE_SEED_PROVIDER = DYNOMITE_PROPS + ".seed.provider";
    private static final String CONFIG_DYNOMITE_CLIENT_PORT = DYNOMITE_PROPS + ".client.port";
    private static final String CONFIG_DYNOMITE_PEER_PORT = DYNOMITE_PROPS + ".peer.port";
    private static final String CONFIG_RACK_NAME = DYNOMITEMANAGER_PRE + ".dyno.rack";
    private static final String CONFIG_USE_ASG_FOR_RACK_NAME = DYNOMITEMANAGER_PRE + ".dyno.asg.rack";
    private static final String CONFIG_TOKENS_DISTRIBUTION_NAME = DYNOMITEMANAGER_PRE + ".dyno.tokens.distribution";
    private static final String CONFIG_DYNO_REQ_TIMEOUT_NAME = DYNOMITEMANAGER_PRE + ".dyno.request.timeout"; // in
													      // milliseconds
    private static final String CONFIG_DYNOMITE_GOSSIP_INTERVAL = DYNOMITE_PROPS + ".gossip.interval"; // in ms
    private static final String CONFIG_DYNOMITE_HASH_ALGORITHM = DYNOMITE_PROPS + ".hash.algorithm";
    private static final String CONFIG_DYNOMITE_STORAGE_PRECONNECT = DYNOMITE_PROPS + ".storage.preconnect";
    private static final String CONFIG_DYNOMITE_MULTI_DC = DYNOMITE_PROPS + ".multi.dc";

    private static final String CONFIG_DYNOMITE_MBUF_SIZE = DYNOMITE_PROPS + ".mbuf.size";
    private static final String CONFIG_DYNOMITE_MAX_ALLOCATED_MESSAGES = DYNOMITE_PROPS + ".max.allocated.messages";

    private static final String CONFIG_AVAILABILITY_ZONES = DYNOMITEMANAGER_PRE + ".zones.available";
    private static final String CONFIG_AVAILABILITY_RACKS = DYNOMITEMANAGER_PRE + ".racks.available";

    private static final String CONFIG_DYNOMITE_PROCESS_NAME = DYNOMITE_PROPS + ".process.name";
    private static final String CONFIG_DYNOMITE_YAML = DYNOMITE_PROPS + ".yaml";
    private static final String CONFIG_DYNOMITE_INTRA_CLUSTER_SECURITY = DYNOMITE_PROPS + ".intra.cluster.security";
    private static final String CONFIG_DYNOMITE_AUTO_EJECT_HOSTS = DYNOMITE_PROPS + ".auto.eject.hosts";

    private static final String CONFIG_DYNOMITE_READ_CONSISTENCY = DYNOMITE_PROPS + ".read.consistency";
    private static final String CONFIG_DYNOMITE_WRITE_CONSISTENCY = DYNOMITE_PROPS + ".write.consistency";

    // Cassandra
    // =========
    // Cassandra is used for token management.

    private static final String CONFIG_CASSANDRA_CLUSTER_NAME = CASSANDRA_PROPS + ".cluster.name";
    private static final String CONFIG_CASSANDRA_KEYSPACE_NAME = CASSANDRA_PROPS + ".keyspace.name";
    private static final String CONFIG_CASSANDRA_SEEDS = CASSANDRA_PROPS + ".seeds";
    private static final String CONFIG_CASSANDRA_THRIFT_PORT = CASSANDRA_PROPS + ".thrift.port";

    // Data store (aka backend)
    // ========================

    // The max percentage of system memory to be allocated to the backend data storage engine (ex. Redis, ARDB).
    private static final String CONFIG_DATASTORE_MAX_MEMORY_PERCENT = DATASTORE_PROPS + ".max.memory.percent";

    // Data store: Redis
    // =================

    private static final String CONFIG_REDIS_CONF = REDIS_PROPS + ".conf";
    private static final String CONFIG_REDIS_DATA_DIR = REDIS_PROPS + ".data.dir";
    private static final String CONFIG_REDIS_PERSISTENCE_ENABLED = REDIS_PROPS + ".persistence.enabled";
    private static final String CONFIG_REDIS_START_SCRIPT = REDIS_PROPS + ".start.script";
    private static final String CONFIG_REDIS_STOP_SCRIPT = REDIS_PROPS + ".stop.script";

    // Data store: ARDB with RocksDB
    // =============================

    private static final String CONFIG_ARDB_ROCKSDB_CONF = ARDB_ROCKSDB_PROPS + ".conf";
    private static final String CONFIG_ARDB_ROCKSDB_MAX_WRITE_BUFFER_NUMBER =
            ARDB_ROCKSDB_PROPS + ".max.write.buffer.number";
    private static final String CONFIG_ARDB_ROCKSDB_MIN_MEMTABLES_TO_MERGE =
            ARDB_ROCKSDB_PROPS + ".min.write.buffer.number.to.merge";
    private static final String CONFIG_ARDB_ROCKSDB_START_SCRIPT = ARDB_ROCKSDB_PROPS + ".start.script";
    private static final String CONFIG_ARDB_ROCKSDB_STOP_SCRIPT = ARDB_ROCKSDB_PROPS + ".stop.script";
    private static final String CONFIG_ARDB_ROCKSDB_WRITE_BUFFER_SIZE = ARDB_ROCKSDB_PROPS + ".write.buffer.size";



    // Eureka
    private static final String CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED = DYNOMITEMANAGER_PRE
	    + ".eureka.host.supplier.enabled";

    // Amazon specific
    private static final String CONFIG_ASG_NAME = DYNOMITEMANAGER_PRE + ".az.asgname";
    private static final String CONFIG_REGION_NAME = DYNOMITEMANAGER_PRE + ".az.region";
    private static final String CONFIG_ACL_GROUP_NAME = DYNOMITEMANAGER_PRE + ".acl.groupname";
    private static final String CONFIG_VPC = DYNOMITEMANAGER_PRE + ".vpc";

    // Dual Account
    private static final String CONFIG_EC2_ROLE_ASSUMPTION_ARN = DYNOMITEMANAGER_PRE + ".ec2.roleassumption.arn";
    private static final String CONFIG_VPC_ROLE_ASSUMPTION_ARN = DYNOMITEMANAGER_PRE + ".vpc.roleassumption.arn";
    private static final String CONFIG_DUAL_ACCOUNT = DYNOMITEMANAGER_PRE + ".roleassumption.dualaccount";
    private static final String CONFIG_DUAL_ACCOUNT_AZ = DYNOMITEMANAGER_PRE + ".roleassumption.az";

    // warm up
    private static final String CONFIG_DYNO_WARM_FORCE = DYNOMITEMANAGER_PRE + ".dyno.warm.force";
    private static final String CONFIG_DYNO_WARM_BOOTSTRAP = DYNOMITEMANAGER_PRE + ".dyno.warm.bootstrap";
    private static final String CONFIG_DYNO_ALLOWABLE_BYTES_SYNC_DIFF = DYNOMITEMANAGER_PRE
	    + ".dyno.warm.bytes.sync.diff";
    private static final String CONFIG_DYNO_MAX_TIME_BOOTSTRAP = DYNOMITEMANAGER_PRE + ".dyno.warm.msec.bootstraptime";

    // Backup and Restore
    private static final String CONFIG_BACKUP_ENABLED = DYNOMITEMANAGER_PRE + ".dyno.backup.snapshot.enabled";
    private static final String CONFIG_BUCKET_NAME = DYNOMITEMANAGER_PRE + ".dyno.backup.bucket.name";
    private static final String CONFIG_S3_BASE_DIR = DYNOMITEMANAGER_PRE + ".dyno.backup.s3.base_dir";
    private static final String CONFIG_BACKUP_HOUR = DYNOMITEMANAGER_PRE + ".dyno.backup.hour";
    private static final String CONFIG_BACKUP_SCHEDULE = DYNOMITEMANAGER_PRE + ".dyno.backup.schedule";
    private static final String CONFIG_RESTORE_ENABLED = DYNOMITEMANAGER_PRE + ".dyno.backup.restore.enabled";
    private static final String CONFIG_RESTORE_TIME = DYNOMITEMANAGER_PRE + ".dyno.backup.restore.date";

    // VPC
    private static final String CONFIG_INSTANCE_DATA_RETRIEVER = DYNOMITEMANAGER_PRE + ".instanceDataRetriever";

    // Defaults: Dynomite
    // ==================

    private final boolean DEFAULT_DYNOMITE_AUTO_EJECT_HOSTS = true;
    private final String DEFAULT_DYNOMITE_CLUSTER_NAME = "dynomite_demo1";
    private final String DEFAULT_DYNOMITE_SEED_PROVIDER = "florida_provider";
    private final String DEFAULT_DYNOMITE_INSTALL_DIR = "/apps/dynomite";
    private final String DEFAULT_DYNOMITE_START_SCRIPT = "/apps/dynomite/bin/launch_dynomite.sh";
    private final String DEFAULT_DYNOMITE_STOP_SCRIPT = "/apps/dynomite/bin/kill_dynomite.sh";
    private final String DEFAULT_DYNOMITE_YAML = "/apps/dynomite/conf/dynomite.yml";

    private List<String> DEFAULT_AVAILABILITY_ZONES = ImmutableList.of();
    private List<String> DEFAULT_AVAILABILITY_RACKS = ImmutableList.of();

    private final String DEFAULT_DYNOMITE_PROCESS_NAME = "dynomite";
    private final int DEFAULT_DYNOMITE_CLIENT_PORT = 8102; // dyn_listen
    private final int DEFAULT_DYNOMITE_PEER_PORT = 8101;
    private final String DEFAULT_DYN_RACK = "RAC1";
    private final String DEFAULT_TOKENS_DISTRIBUTION = "vnode";
    private final int DEFAULT_DYNO_REQ_TIMEOUT_IN_MILLISEC = 5000;
    private final int DEFAULT_DYNOMITE_GOSSIP_INTERVAL = 10000;
    private final String DEFAULT_DYNOMITE_HASH_ALGORITHM = "murmur";

    private final String DEFAULT_DYNOMITE_INTRA_CLUSTER_SECURITY = "datacenter";

    private final String DEFAULT_DYNOMITE_READ_CONSISTENCY = "DC_ONE";
    private final String DEFAULT_DYNOMITE_WRITE_CONSISTENCY = "DC_ONE";

    // Backup & Restore
    private static final boolean DEFAULT_BACKUP_ENABLED = false;
    private static final boolean DEFAULT_RESTORE_ENABLED = false;
    // private static final String DEFAULT_BUCKET_NAME =
    // "us-east-1.dynomite-backup-test";
    private static final String DEFAULT_BUCKET_NAME = "dynomite-backup";

    private static final String DEFAULT_BUCKET_FOLDER = "backup";
    private static final String DEFAULT_RESTORE_REPOSITORY_TYPE = "s3";

    private static final String DEFAULT_RESTORE_SNAPSHOT_NAME = "";
    private static final String DEFAULT_RESTORE_SOURCE_REPO_REGION = "us-east-1";
    private static final String DEFAULT_RESTORE_SOURCE_CLUSTER_NAME = "";
    private static final String DEFAULT_RESTORE_REPOSITORY_NAME = "testrepo";
    private static final String DEFAULT_RESTORE_TIME = "20101010";
    private static final String DEFAULT_BACKUP_SCHEDULE = "day";
    private static final int DEFAULT_BACKUP_HOUR = 12;

    // AWS Dual Account
    private static final boolean DEFAULT_DUAL_ACCOUNT = false;

    private static final Logger logger = LoggerFactory.getLogger(DynomiteManagerConfiguration.class);

    private final String AUTO_SCALE_GROUP_NAME = System.getenv("AUTO_SCALE_GROUP");
    private static final String DEFAULT_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.AwsInstanceDataRetriever";
    private static final String VPC_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.VpcInstanceDataRetriever";

    private static String ASG_NAME = System.getenv("ASG_NAME");

    private final InstanceDataRetriever retriever;
    private final ICredential provider;
    private final IConfigSource configSource;
    private final InstanceEnvIdentity insEnvIdentity;
    private final IStorageProxy storageProxy;

    // Defaults: Cassandra
    // ===================

    private static final String DEFAULT_CASSANDRA_CLUSTER_NAME = "cass_dyno";
    private static final String DEFAULT_CASSANDRA_KEYSPACE_NAME = "dyno_bootstrap";
    private static final String DEFAULT_CASSANDRA_SEEDS = "127.0.0.1"; // comma separated list
    private static final int DEFAULT_CASSANDRA_THRIFT_PORT = 9160; // 7102;

    // Defaults: Data store: Redis
    // ===========================

    private static final String DEFAULT_REDIS_CONF = "/apps/nfredis/conf/redis.conf";
    private static final String DEFAULT_REDIS_DATA_DIR = "/mnt/data/nfredis";
    private static final boolean DEFAULT_REDIS_PERSISTENCE_ENABLED = false;
    private static final String DEFAULT_REDIS_START_SCRIPT = "/apps/nfredis/bin/launch_nfredis.sh";
    private static final String DEFAULT_REDIS_STOP_SCRIPT = "/apps/nfredis/bin/kill_redis.sh";

    // Defaults: Data store: ARDB with RocksDB
    // =======================================

    private static final String DEFAULT_ARDB_ROCKSDB_CONF = "/apps/ardb/conf/rocksdb.conf";
    private static final int DEFAULT_ARDB_ROCKSDB_MAX_WRITE_BUFFER_NUMBER = 16;
    private static final int DEFAULT_ARDB_ROCKSDB_MIN_MEMTABLES_TO_MERGE = 4;
    private static final String DEFAULT_ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
    private static final String DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT = "/apps/ardb/bin/kill_ardb.sh";
    private static final int DEFAULT_ARDB_ROCKSDB_WRITE_BUFFER_SIZE = 128; // MB


    private static final boolean DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED = true;

    // = instance identity meta data
    private String RAC, ZONE, PUBLIC_HOSTNAME, PUBLIC_IP, INSTANCE_ID, INSTANCE_TYPE;
    private String NETWORK_MAC; // Fetch metadata of the running instance's
				// network interface

    // == vpc specific
    private String NETWORK_VPC; // Fetch the vpc id of running instance

    @Inject
    public DynomiteManagerConfiguration(ICredential provider, IConfigSource configSource,
	    InstanceDataRetriever retriever, InstanceEnvIdentity insEnvIdentity,
	    IStorageProxy storageProxy) {
	this.retriever = retriever;
	this.provider = provider;
	this.configSource = configSource;
	this.insEnvIdentity = insEnvIdentity;
	this.storageProxy = storageProxy;

	RAC = retriever.getRac();
	ZONE = RAC;
	PUBLIC_HOSTNAME = retriever.getPublicHostname();
	PUBLIC_IP = retriever.getPublicIP();

	INSTANCE_ID = retriever.getInstanceId();
	INSTANCE_TYPE = retriever.getInstanceType();

	NETWORK_MAC = retriever.getMac();
	if (insEnvIdentity.isNonDefaultVpc() || insEnvIdentity.isDefaultVpc()) {
	    NETWORK_VPC = retriever.getVpcId();
	    logger.info("vpc id for running instance: " + NETWORK_VPC);
	}
    }

    private InstanceDataRetriever getInstanceDataRetriever()
	    throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	String s = null;

	if (this.insEnvIdentity.isClassic()) {
	    s = this.configSource.get(CONFIG_INSTANCE_DATA_RETRIEVER, DEFAULT_INSTANCE_DATA_RETRIEVER);

	} else if (this.insEnvIdentity.isNonDefaultVpc()) {
	    s = this.configSource.get(CONFIG_INSTANCE_DATA_RETRIEVER, VPC_INSTANCE_DATA_RETRIEVER);
	} else {
	    logger.error("environment cannot be found");
	    throw new IllegalStateException("Unable to determine environemt (vpc, classic) for running instance.");
	}
	return (InstanceDataRetriever) Class.forName(s).newInstance();

    }

    /**
     * Set Dynomite Manager's configuration options.
     */
    public void initialize() {
        setupEnvVars();
        this.configSource.initialize(ASG_NAME, getDataCenter());
        setDefaultRACList(getDataCenter());
    }

    /**
     * Set configuration options provided by environment variables or Java
     * properties. Java properties are only used if the equivalent environment
     * variable is not set.
     *
     * Environment variables and Java properties are applied in the following
     * order:
     * <ol>
     * <li>Environment variable: Preferred value
     * <li>Java property: If environment variable is not set, then Java property
     * is used.
     * </ol>
     */
    private void setupEnvVars() {
        // Search in java opt properties
        try {
            logger.info("Setting up environmental variables and Java properties.");
            ASG_NAME = StringUtils.isBlank(ASG_NAME) ? System.getProperty("ASG_NAME") : ASG_NAME;
            if (StringUtils.isBlank(ASG_NAME))
                ASG_NAME = populateASGName(getDataCenter(), this.retriever.getInstanceId());
            logger.info(String.format("REGION set to %s, ASG Name set to %s", getDataCenter(), ASG_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Query Amazon to get ASG name. Currently not available as part of instance
     * info api.
     */
    private String populateASGName(String region, String instanceId) {
	GetASGName getASGName = new GetASGName(region, instanceId);

	try {
	    return getASGName.call();
	} catch (Exception e) {
	    logger.error("Failed to determine ASG name.", e);
	    return null;
	}
    }

    private class GetASGName extends RetryableCallable<String> {
	private static final int NUMBER_OF_RETRIES = 15;
	private static final long WAIT_TIME = 30000;
	private final String instanceId;
	private final AmazonEC2 client;

	public GetASGName(String region, String instanceId) {
	    super(NUMBER_OF_RETRIES, WAIT_TIME);
	    this.instanceId = instanceId;
	    client = new AmazonEC2Client(provider.getAwsCredentialProvider());
	    client.setEndpoint("ec2." + region + ".amazonaws.com");
	}

	@Override
	public String retriableCall() throws IllegalStateException {
	    DescribeInstancesRequest desc = new DescribeInstancesRequest().withInstanceIds(instanceId);
	    DescribeInstancesResult res = client.describeInstances(desc);

	    for (Reservation resr : res.getReservations()) {
		for (Instance ins : resr.getInstances()) {
		    for (com.amazonaws.services.ec2.model.Tag tag : ins.getTags()) {
			if (tag.getKey().equals("aws:autoscaling:groupName"))
			    return tag.getValue();
		    }
		}
	    }

	    logger.warn("Couldn't determine ASG name");
	    throw new IllegalStateException("Couldn't determine ASG name");
	}
    }

    private boolean useAsgForRackName() {
	return configSource.get(CONFIG_USE_ASG_FOR_RACK_NAME, true);
    }

    @Override
    public String getZone() {
	return ZONE;
    }

    @Override
    public String getHostname() {
	return PUBLIC_HOSTNAME;
    }

    @Override
    public String getInstanceName() {
	return INSTANCE_ID;
    }

    @Override
    public String getRack() {
	if (useAsgForRackName()) {
	    return getASGName();
	}

	return configSource.get(CONFIG_RACK_NAME, DEFAULT_DYN_RACK);
    }

    @Override
    public List<String> getZones() {
	return configSource.getList(CONFIG_AVAILABILITY_ZONES, DEFAULT_AVAILABILITY_ZONES);
    }

    @Override
    public String getCrossAccountRack() {
	// If a fast property is not set, it uses the local rack name
	return configSource.get(CONFIG_DUAL_ACCOUNT_AZ, getRack());
    }

    public List<String> getRacks() {
	return configSource.getList(CONFIG_AVAILABILITY_RACKS, DEFAULT_AVAILABILITY_RACKS);
    }

    public String getDataCenter() {
        String dcEnv = System.getenv("EC2_REGION");
        String dcMetadata = retriever.getDataCenter();
        String dcConfig = configSource.get(CONFIG_REGION_NAME, "");

        if (dcEnv != null && !dcEnv.isEmpty()) {
            return dcEnv;
        } else if (dcMetadata != null && !dcMetadata.isEmpty()) {
            return dcMetadata;
        } else if (dcConfig != null && !dcConfig.isEmpty()) {
            return dcConfig;
        }

        return dcMetadata;
    }

    @Override
    public String getASGName() {
	return AUTO_SCALE_GROUP_NAME;
    }

    /**
     * Get the fist 3 available zones in the region
     */
    public void setDefaultRACList(String region) {
	AmazonEC2 client = new AmazonEC2Client(provider.getAwsCredentialProvider());
	client.setEndpoint("ec2." + region + ".amazonaws.com");
	DescribeAvailabilityZonesResult res = client.describeAvailabilityZones();
	List<String> zone = Lists.newArrayList();
	for (AvailabilityZone reg : res.getAvailabilityZones()) {
	    if (reg.getState().equals("available"))
		zone.add(reg.getZoneName());
	    if (zone.size() == 3)
		break;
	}
	// DEFAULT_AVAILABILITY_ZONES = StringUtils.join(zone, ",");
	DEFAULT_AVAILABILITY_ZONES = ImmutableList.copyOf(zone);
    }

    @Override
    public String getACLGroupName() {
        return configSource.get(CONFIG_ACL_GROUP_NAME, this.getDynomiteClusterName());
    }

    @Override
    public String getHostIP() {
	return PUBLIC_IP;
    }

    @Override
    public String getDistribution() {
	return configSource.get(CONFIG_TOKENS_DISTRIBUTION_NAME, DEFAULT_TOKENS_DISTRIBUTION);
    }

    // Dynomite
    // ========

    @Override
    public boolean getDynomiteAutoEjectHosts() {
        return getBooleanProperty("DM_DYNOMITE_AUTO_EJECT_HOSTS", CONFIG_DYNOMITE_AUTO_EJECT_HOSTS,
                DEFAULT_DYNOMITE_AUTO_EJECT_HOSTS);
    }

    @Override
    public int getDynomiteClientPort() {
        return getIntProperty("DM_DYNOMITE_CLIENT_PORT", CONFIG_DYNOMITE_CLIENT_PORT, DEFAULT_DYNOMITE_CLIENT_PORT);
    }

    @Override
    public String getDynomiteClusterName() {
        // Maintain backward compatibility for env var
        String clusterNameOldEnvVar = System.getenv("NETFLIX_APP");
        if (clusterNameOldEnvVar != null && !"".equals(clusterNameOldEnvVar)) {
            logger.warn("NETFLIX_APP is deprecated. Use DM_DYNOMITE_CLUSTER_NAME.");
            return clusterNameOldEnvVar;
        }

        return getStringProperty("DM_DYNOMITE_CLUSTER_NAME", CONFIG_DYNOMITE_CLUSTER_NAME,
                DEFAULT_DYNOMITE_CLUSTER_NAME);
    }

    @Override
    public int getDynomiteGossipInterval() {
        return getIntProperty("DM_DYNOMITE_GOSSIP_INTERVAL", CONFIG_DYNOMITE_GOSSIP_INTERVAL,
                DEFAULT_DYNOMITE_GOSSIP_INTERVAL);
    }

    @Override
    public String getDynomiteHashAlgorithm() {
        return getStringProperty("DM_DYNOMITE_HASH_ALGORITHM", CONFIG_DYNOMITE_HASH_ALGORITHM,
                DEFAULT_DYNOMITE_HASH_ALGORITHM);
    }

    @Override
    public String getDynomiteInstallDir() {
        return getStringProperty("DM_DYNOMITE_INSTALL_DIR", CONFIG_DYNOMITE_INSTALL_DIR, DEFAULT_DYNOMITE_INSTALL_DIR);
    }

    public String getDynomiteIntraClusterSecurity() {
        return getStringProperty("DM_DYNOMITE_INTRA_CLUSTER_SECURITY", CONFIG_DYNOMITE_INTRA_CLUSTER_SECURITY,
                DEFAULT_DYNOMITE_INTRA_CLUSTER_SECURITY);
    }

    @Override
    public String getClientListenPort() {
        return "0.0.0.0:" + getDynomiteClientPort();
    }

    public int getDynomiteMaxAllocatedMessages() {
        return getIntProperty("DM_DYNOMITE_MAX_ALLOCATED_MESSAGES", CONFIG_DYNOMITE_MAX_ALLOCATED_MESSAGES, 200000);
    }

    @Override
    public int getDynomitePeerPort() {
        return getIntProperty("DM_DYNOMITE_PEER_PORT", CONFIG_DYNOMITE_PEER_PORT, DEFAULT_DYNOMITE_PEER_PORT);
    }

    public int getDynomiteMBufSize() {
        return getIntProperty("DM_DYNOMITE_MBUF_SIZE", CONFIG_DYNOMITE_MBUF_SIZE, 16384);
    }

    @Override
    public String getDynomiteProcessName() {
        return getStringProperty("DM_DYNOMITE_PROCESS_NAME", CONFIG_DYNOMITE_PROCESS_NAME,
                DEFAULT_DYNOMITE_PROCESS_NAME);
    }

    public String getDynomiteReadConsistency() {
        return getStringProperty("DM_DYNOMITE_READ_CONSISTENCY", CONFIG_DYNOMITE_READ_CONSISTENCY,
                DEFAULT_DYNOMITE_READ_CONSISTENCY);
    }

    @Override
    public String getDynomiteSeedProvider() {
        return getStringProperty("DM_DYNOMITE_SEED_PROVIDER", CONFIG_DYNOMITE_SEED_PROVIDER,
                DEFAULT_DYNOMITE_SEED_PROVIDER);
    }

    public String getDynomiteStartScript() {
        return getStringProperty("DM_DYNOMITE_START_SCRIPT", CONFIG_DYNOMITE_START_SCRIPT,
                DEFAULT_DYNOMITE_START_SCRIPT);
    }

    public String getDynomiteStopScript() {
        return getStringProperty("DM_DYNOMITE_STOP_SCRIPT", CONFIG_DYNOMITE_STOP_SCRIPT, DEFAULT_DYNOMITE_STOP_SCRIPT);
    }

    @Override
    public boolean getDynomiteStoragePreconnect() {
        return getBooleanProperty("DM_DYNOMITE_STORAGE_PRECONNECT", CONFIG_DYNOMITE_STORAGE_PRECONNECT, true);
    }

    public String getDynomiteWriteConsistency() {
        return getStringProperty("DM_DYNOMITE_WRITE_CONSISTENCY", CONFIG_DYNOMITE_WRITE_CONSISTENCY,
                DEFAULT_DYNOMITE_WRITE_CONSISTENCY);
    }

    public String getDynomiteYaml() {
        String dynomiteYaml = getStringProperty("DM_DYNOMITE_YAML", CONFIG_DYNOMITE_YAML, DEFAULT_DYNOMITE_YAML);
        // If a user sets a relative path to dynomite.yaml then we need to prepend the Dynomite installation directory
        // in order to return a full path from /.
        if (dynomiteYaml.charAt(0) == '/') {
            return dynomiteYaml;
        } else {
            return getDynomiteInstallDir() + "/" + dynomiteYaml;
        }
    }

    public boolean isDynomiteMultiDC() {
        return getBooleanProperty("DM_DYNOMITE_MULTI_DC", CONFIG_DYNOMITE_MULTI_DC, true);
    }

    @Override
    public String getDynListenPort() { // return full string
	return "0.0.0.0:" + getDynomitePeerPort();
    }

    @Override
    public int getServerRetryTimeout() {
	return 30000;
    }

    @Override
    public int getTimeout() {
	return configSource.get(CONFIG_DYNO_REQ_TIMEOUT_NAME, DEFAULT_DYNO_REQ_TIMEOUT_IN_MILLISEC);
    }

    public boolean isWarmBootstrap() {
	return configSource.get(CONFIG_DYNO_WARM_BOOTSTRAP, false);
    }

    public boolean isForceWarm() {
	return configSource.get(CONFIG_DYNO_WARM_FORCE, false);
    }

    public int getAllowableBytesSyncDiff() {
	return configSource.get(CONFIG_DYNO_ALLOWABLE_BYTES_SYNC_DIFF, 100000);
    }

    public int getMaxTimeToBootstrap() {
	return configSource.get(CONFIG_DYNO_MAX_TIME_BOOTSTRAP, 900000);
    }

    // Data store (aka backend)
    // ========================

    @Override
    public int getDatastoreMaxMemoryPercent() {
        return getIntProperty("DM_DATASTORE_MAX_MEMORY_PERCENT", CONFIG_DATASTORE_MAX_MEMORY_PERCENT, 85);
    }

    public boolean isVpc() {
	return configSource.get(CONFIG_VPC, false);
    }

    // Backup & Restore Implementations

    @Override
    public String getBucketName() {
	return configSource.get(CONFIG_BUCKET_NAME, DEFAULT_BUCKET_NAME);
    }

    @Override
    public String getBackupLocation() {
	return configSource.get(CONFIG_S3_BASE_DIR, DEFAULT_BUCKET_FOLDER);
    }

    @Override
    public boolean isBackupEnabled() {
	return configSource.get(CONFIG_BACKUP_ENABLED, DEFAULT_BACKUP_ENABLED);
    }

    @Override
    public boolean isRestoreEnabled() {
	return configSource.get(CONFIG_RESTORE_ENABLED, DEFAULT_RESTORE_ENABLED);
    }

    @Override
    public String getBackupSchedule() {
        String configBackupSchedule = configSource.get(CONFIG_BACKUP_SCHEDULE);
	    if (configBackupSchedule != null && !"day".equals(configBackupSchedule)
		    && !"week".equals(configBackupSchedule)) {
	        logger.error("The persistence schedule FP is wrong: day or week");
	        logger.error("Defaulting to " + DEFAULT_BACKUP_SCHEDULE);
	        return DEFAULT_BACKUP_SCHEDULE;
	    }
	    return configSource.get(CONFIG_BACKUP_SCHEDULE);
    }

    @Override
    public int getBackupHour() {
	return configSource.get(CONFIG_BACKUP_HOUR, DEFAULT_BACKUP_HOUR);
    }

    @Override
    public String getRestoreDate() {
	return configSource.get(CONFIG_RESTORE_TIME, DEFAULT_RESTORE_TIME);
    }

    // VPC
    @Override
    public String getVpcId() {
	return NETWORK_VPC;
    }

    @Override
    public String getClassicAWSRoleAssumptionArn() {
	return configSource.get(CONFIG_EC2_ROLE_ASSUMPTION_ARN);
    }

    @Override
    public String getVpcAWSRoleAssumptionArn() {
	return configSource.get(CONFIG_VPC_ROLE_ASSUMPTION_ARN);
    }

    @Override
    public boolean isDualAccount() {
	return configSource.get(CONFIG_DUAL_ACCOUNT, DEFAULT_DUAL_ACCOUNT);
    }

    // Cassandra
    // =========

    @Override
    public String getCassandraClusterName() {
        return getStringProperty("DM_CASSANDRA_CLUSTER_NAME", CONFIG_CASSANDRA_CLUSTER_NAME,
                DEFAULT_CASSANDRA_CLUSTER_NAME);
    }

    @Override
    public String getCassandraKeyspaceName() {
        return getStringProperty("DM_CASSANDRA_KEYSPACE_NAME", CONFIG_CASSANDRA_KEYSPACE_NAME,
                DEFAULT_CASSANDRA_KEYSPACE_NAME);
    }

    @Override
    public String getCassandraSeeds() {
        // Maintain backward compatibility for env var
        String clusterNameOldEnvVar = System.getenv("DM_CASSANDRA_CLUSTER_SEEDS");
        if (clusterNameOldEnvVar != null && !"".equals(clusterNameOldEnvVar)) {
            logger.warn("DM_CASSANDRA_CLUSTER_SEEDS is deprecated. Use DM_CASSANDRA_SEEDS.");
            return clusterNameOldEnvVar;
        }

        return getStringProperty("DM_CASSANDRA_SEEDS", CONFIG_CASSANDRA_SEEDS, DEFAULT_CASSANDRA_SEEDS);
    }

    @Override
    public int getCassandraThriftPort() {
        return getIntProperty("DM_CASSANDRA_THRIFT_PORT", CONFIG_CASSANDRA_THRIFT_PORT, DEFAULT_CASSANDRA_THRIFT_PORT);
    }



    @Override
    public boolean isEurekaHostSupplierEnabled() {
	return configSource.get(CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED, DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED);
    }

    // Data store: Redis
    // =================

    @Override
    public String getRedisConf() {
        return getStringProperty("DM_REDIS_CONF", CONFIG_REDIS_CONF, DEFAULT_REDIS_CONF);
    }

    @Override
    public String getRedisDataDir() {
        return getStringProperty("DM_REDIS_DATA_DIR", CONFIG_REDIS_DATA_DIR, DEFAULT_REDIS_DATA_DIR);
    }

    @Override
    public String getRedisStartScript() {
        return getStringProperty("DM_REDIS_START_SCRIPT", CONFIG_REDIS_START_SCRIPT, DEFAULT_REDIS_START_SCRIPT);
    }

    @Override
    public String getRedisStopScript() {
        return getStringProperty("DM_REDIS_STOP_SCRIPT", CONFIG_REDIS_STOP_SCRIPT, DEFAULT_REDIS_STOP_SCRIPT);
    }

    @Override
    public boolean isRedisPersistenceEnabled() {
        return getBooleanProperty("DM_REDIS_PERSISTENCE_ENABLED", CONFIG_REDIS_PERSISTENCE_ENABLED,
                DEFAULT_REDIS_PERSISTENCE_ENABLED);
    }

    @Override
    public boolean isRedisAofEnabled() {
        final String CONFIG_REDIS_PERSISTENCE_TYPE = DYNOMITEMANAGER_PRE + ".dyno.persistence.type";
        final String DEFAULT_REDIS_PERSISTENCE_TYPE = "aof";

        if (configSource.get(CONFIG_REDIS_PERSISTENCE_TYPE, DEFAULT_REDIS_PERSISTENCE_TYPE).equals("rdb")) {
            return false;
        } else if (configSource.get(CONFIG_REDIS_PERSISTENCE_TYPE, DEFAULT_REDIS_PERSISTENCE_TYPE).equals("aof")) {
            return true;
        } else {
            logger.error("The persistence type FP is wrong: aof or rdb");
            logger.error("Defaulting to rdb");
            return false;
        }
    }

    @Override
    public String getRedisCompatibleEngine() {
        final String DEFAULT_REDIS_COMPATIBLE_SERVER = "redis";
        final String CONFIG_DYNO_REDIS_COMPATIBLE_SERVER = DYNOMITEMANAGER_PRE + ".dyno.redis.compatible.engine";
        return configSource.get(CONFIG_DYNO_REDIS_COMPATIBLE_SERVER, DEFAULT_REDIS_COMPATIBLE_SERVER);
    }

    // Data store: ARDB with RocksDB
    // =============================

    @Override
    public String getArdbRocksDBConf() {
        return getStringProperty("DM_ARDB_ROCKSDB_CONF", CONFIG_ARDB_ROCKSDB_CONF, DEFAULT_ARDB_ROCKSDB_CONF);
    }

    @Override
    public int getArdbRocksDBMaxWriteBufferNumber() {
        return getIntProperty("DM_ARDB_ROCKSDB_MAX_WRITE_BUFFER_NUMBER", CONFIG_ARDB_ROCKSDB_MAX_WRITE_BUFFER_NUMBER,
                DEFAULT_ARDB_ROCKSDB_MAX_WRITE_BUFFER_NUMBER);
    }

    @Override
    public int getArdbRocksDBMinWriteBuffersToMerge() {
        return getIntProperty("DM_ARDB_ROCKSDB_MIN_WRITE_BUFFER_NUMBER_TO_MERGE",
                CONFIG_ARDB_ROCKSDB_MIN_MEMTABLES_TO_MERGE, DEFAULT_ARDB_ROCKSDB_MIN_MEMTABLES_TO_MERGE);
    }

    @Override
    public String getArdbRocksDBStartScript() {
        return getStringProperty("DM_ARDB_ROCKSDB_START_SCRIPT", CONFIG_ARDB_ROCKSDB_START_SCRIPT,
                DEFAULT_ARDB_ROCKSDB_START_SCRIPT);
    }

    @Override
    public String getArdbRocksDBStopScript() {
        return getStringProperty("DM_ARDB_ROCKSDB_STOP_SCRIPT", CONFIG_ARDB_ROCKSDB_STOP_SCRIPT,
                DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT);
    }

    @Override
    public int getArdbRocksDBWriteBufferSize() {
        return getIntProperty("DM_ARDB_ROCKSDB_WRITE_BUFFER_SIZE", CONFIG_ARDB_ROCKSDB_WRITE_BUFFER_SIZE,
                DEFAULT_ARDB_ROCKSDB_WRITE_BUFFER_SIZE);
    }

}
