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

import java.util.List;

import com.netflix.dynomitemanager.sidecore.storage.RedisStorageProxy;
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
public class DynomitemanagerConfiguration implements IConfiguration {
    public static final String DYNOMITEMANAGER_PRE = "florida";

	// Dynomite
	// ========

    public static final String LOCAL_ADDRESS = "127.0.0.1";

    // Cluster name is saved as tokens.appId in Cassandra.
    // The cluster name is used as the default AWS Security Group name, if SG name is null.
    private static final String CONFIG_CLUSTER_NAME = DYNOMITEMANAGER_PRE + ".dyno.clustername";
    private static final String CONFIG_RACK_NAME = DYNOMITEMANAGER_PRE + ".dyno.rack";
    private static final String CONFIG_USE_ASG_FOR_RACK_NAME = DYNOMITEMANAGER_PRE + ".dyno.asg.rack";
    private static final String CONFIG_DYNO_IS_MULTI_REGIONED_CLUSTER = DYNOMITEMANAGER_PRE + ".dyno.multiregion";

    // ASA: RedisConfiguration
    // The max percentage of system memory to be allocated to the Dynomite
    // fronted data store.
    private static final String CONFIG_DYNO_STORAGE_MEM_PCT_INT = DYNOMITEMANAGER_PRE + ".dyno.storage.mem.pct.int";

    // ASA: AwsConfiguration (duplicate)
    private static final String CONFIG_AVAILABILITY_ZONES = DYNOMITEMANAGER_PRE + ".zones.available";
    private static final String CONFIG_AVAILABILITY_RACKS = DYNOMITEMANAGER_PRE + ".racks.available";

    // Cassandra Cluster for token management
    private static final String CONFIG_BOOTCLUSTER_NAME = DYNOMITEMANAGER_PRE + ".bootcluster";
    private static final String CONFIG_CASSANDRA_KEYSPACE_NAME = DYNOMITEMANAGER_PRE + ".cassandra.keyspace.name";
    private static final String CONFIG_CASSANDRA_THRIFT_PORT = DYNOMITEMANAGER_PRE + ".cassandra.thrift.port";
    private static final String CONFIG_CASSANDRA_HOSTNAMES = DYNOMITEMANAGER_PRE
            + ".cassandra.comma.separated.hostnames";

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

    // RocksDB
    private static final String CONFIG_WRITE_BUFFER_SIZE_MB = DYNOMITEMANAGER_PRE + ".dyno.ardb.rocksdb.writebuffermb";
    private static final String CONFIG_MAX_WRITE_BUFFER_NUMBER = DYNOMITEMANAGER_PRE + ".dyno.ardb.rocksdb.maxwritebuffernumber";
    private static final String CONFIG_MIN_WRITE_BUFFER_NAME_TO_MERGE = DYNOMITEMANAGER_PRE + ".dyno.ardb.rocksdb.minwritebuffernametomerge";

    // Defaults
    private final String DEFAULT_CLUSTER_NAME = "dynomite_demo1";

    private List<String> DEFAULT_AVAILABILITY_ZONES = ImmutableList.of();
    private List<String> DEFAULT_AVAILABILITY_RACKS = ImmutableList.of();

    private final String DEFAULT_DYN_RACK = "RAC1";

    private final String DEFAULT_METADATA_KEYSPACE = "dyno_bootstrap";

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

    // Ardb
    private static final int DEFAULT_WRITE_BUFFER_SIZE_MB = 128;
    private static final int DEFAULT_MAX_WRITE_BUFFER_NUMBER = 16;
    private static final int DEFAULT_MIN_WRITE_BUFFER_NAME_TO_MERGE = 4;

    // AWS Dual Account
    private static final boolean DEFAULT_DUAL_ACCOUNT = false;

    private static final Logger logger = LoggerFactory.getLogger(DynomitemanagerConfiguration.class);

    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // ASA: IDENTICAL VALUE TO ASG_NAME
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    private final String AUTO_SCALE_GROUP_NAME = System.getenv("AUTO_SCALE_GROUP");
    private static final String DEFAULT_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.AwsInstanceDataRetriever";
    private static final String VPC_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.VpcInstanceDataRetriever";

    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // ASA: IDENTICAL VALUE TO AUTO_SCALE_GROUP_NAME
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    // =============================================
    private static String ASG_NAME = System.getenv("ASG_NAME");

    private final InstanceDataRetriever retriever;
    private final ICredential provider;
    private final IConfigSource configSource;
    private final InstanceEnvIdentity insEnvIdentity;
    private final IStorageProxy storageProxy;

    // Cassandra default configuration
    private static final String DEFAULT_BOOTCLUSTER_NAME = "cass_dyno";
    private static final int DEFAULT_CASSANDRA_THRIFT_PORT = 9160; // 7102;
    private static final String DEFAULT_CASSANDRA_KEYSPACE_NAME = "dyno_bootstrap";
    private static final String DEFAULT_CASSANDRA_HOSTNAMES = "127.0.0.1";
    private static final boolean DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED = true;

    // = instance identity meta data
    private String RAC, ZONE, PUBLIC_HOSTNAME, PUBLIC_IP, INSTANCE_ID, INSTANCE_TYPE;
    private String NETWORK_MAC; // Fetch metadata of the running instance's
				// network interface

    // == vpc specific
    private String NETWORK_VPC; // Fetch the vpc id of running instance

    @Inject
    public DynomitemanagerConfiguration(ICredential provider, IConfigSource configSource,
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
    public String getAppName() {
        String clusterName = System.getenv("NETFLIX_APP");

        if (StringUtils.isBlank(clusterName))
            return configSource.get(CONFIG_CLUSTER_NAME, DEFAULT_CLUSTER_NAME);

        return clusterName;
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

    // ASA: Unused (may be a duplicate). Similar to getRacks().
    @Override
    public List<String> getZones() {
	return configSource.getList(CONFIG_AVAILABILITY_ZONES, DEFAULT_AVAILABILITY_ZONES);
    }

    @Override
    public String getCrossAccountRack() {
	// If a fast property is not set, it uses the local rack name
	return configSource.get(CONFIG_DUAL_ACCOUNT_AZ, getRack());
    }

    // ASA: Similar to getZones()
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
        return configSource.get(CONFIG_ACL_GROUP_NAME, this.getAppName());
    }

    @Override
    public String getHostIP() {
	return PUBLIC_IP;
    }

    @Override
    public String getBootClusterName() {
        return configSource.get(CONFIG_BOOTCLUSTER_NAME, DEFAULT_BOOTCLUSTER_NAME);
    }

    @Override
    public String getTokens() {
	// TODO Auto-generated method stub
	return null;
    }

    // ASA: Should this be moved to Dynomite?
    public boolean isMultiRegionedCluster() {
	return configSource.get(CONFIG_DYNO_IS_MULTI_REGIONED_CLUSTER, true);
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

    // ASA: RedisConfiguration
    @Override
    public int getStorageMemPercent() {
	return configSource.get(CONFIG_DYNO_STORAGE_MEM_PCT_INT, 85);
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
	if (CONFIG_BACKUP_SCHEDULE != null && !"day".equals(CONFIG_BACKUP_SCHEDULE)
		&& !"week".equals(CONFIG_BACKUP_SCHEDULE)) {

	    logger.error("The persistence schedule FP is wrong: day or week");
	    logger.error("Defaulting to day");
	    return configSource.get("day", DEFAULT_BACKUP_SCHEDULE);
	}
	return configSource.get(CONFIG_BACKUP_SCHEDULE, DEFAULT_BACKUP_SCHEDULE);
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

    // RocksDB
    @Override
    public int getWriteBufferSize() {
	return configSource.get(CONFIG_WRITE_BUFFER_SIZE_MB,DEFAULT_WRITE_BUFFER_SIZE_MB);
    }

    public int getMaxWriteBufferNumber() {
	return configSource.get(CONFIG_MAX_WRITE_BUFFER_NUMBER,DEFAULT_MAX_WRITE_BUFFER_NUMBER);
    }

    public int getMinWriteBufferToMerge() {
	return configSource.get(CONFIG_MIN_WRITE_BUFFER_NAME_TO_MERGE,DEFAULT_MIN_WRITE_BUFFER_NAME_TO_MERGE);
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

    // Cassandra configuration for token management
    @Override
    public String getCassandraKeyspaceName() {
	return configSource.get(CONFIG_CASSANDRA_KEYSPACE_NAME, DEFAULT_CASSANDRA_KEYSPACE_NAME);
    }

    @Override
    public int getCassandraThriftPortForAstyanax() {
        return configSource.get(CONFIG_CASSANDRA_THRIFT_PORT, DEFAULT_CASSANDRA_THRIFT_PORT);
    }

    @Override
    public String getCassandraHostNames() {
        return configSource.get(CONFIG_CASSANDRA_HOSTNAMES, DEFAULT_CASSANDRA_HOSTNAMES);
    }

    @Override
    public boolean isEurekaHostSupplierEnabled() {
	return configSource.get(CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED, DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED);
    }

    // Redis
    // =====

    @Override
    public String getRedisConf() {
        final String DEFAULT_REDIS_CONF = "/apps/nfredis/conf/redis.conf";
        final String CONFIG_REDIS_CONF = DYNOMITEMANAGER_PRE + ".redis.conf";
        return configSource.get(CONFIG_REDIS_CONF, DEFAULT_REDIS_CONF);
    }

    @Override
    public String getRedisInitStart() {
        final String DEFAULT_REDIS_START_SCRIPT = "/apps/nfredis/bin/launch_nfredis.sh";
        final String CONFIG_REDIS_START_SCRIPT = DYNOMITEMANAGER_PRE + ".redis.init.start";
        return configSource.get(CONFIG_REDIS_START_SCRIPT, DEFAULT_REDIS_START_SCRIPT);
    }

    @Override
    public String getRedisInitStop() {
        final String DEFAULT_REDIS_STOP_SCRIPT = "/apps/nfredis/bin/kill_redis.sh";
        final String CONFIG_REDIS_STOP_SCRIPT = DYNOMITEMANAGER_PRE + ".redis.init.stop";
        return configSource.get(CONFIG_REDIS_STOP_SCRIPT, DEFAULT_REDIS_STOP_SCRIPT);
    }

    @Override
    public boolean isRedisPersistenceEnabled() {
        final boolean DEFAULT_REDIS_PERSISTENCE_ENABLED = false;
        final String CONFIG_REDIS_PERSISTENCE_ENABLED = DYNOMITEMANAGER_PRE + ".dyno.persistence.enabled";
        return configSource.get(CONFIG_REDIS_PERSISTENCE_ENABLED, DEFAULT_REDIS_PERSISTENCE_ENABLED);
    }

    @Override
    public String getRedisDataDir() {
        final String DEFAULT_REDIS_DATA_DIR = "/mnt/data/nfredis";
        final String CONFIG_REDIS_DATA_DIR = DYNOMITEMANAGER_PRE + ".dyno.persistence.directory";
        return configSource.get(CONFIG_REDIS_DATA_DIR, DEFAULT_REDIS_DATA_DIR);
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

    // ARDB RocksDB
    // ============

    @Override
    public String getArdbRocksDBConf() {
        String DEFAULT_ARDB_ROCKSDB_CONF = "/apps/ardb/conf/rocksdb.conf";
        final String CONFIG_ARDB_ROCKSDB_CONF = DYNOMITEMANAGER_PRE + ".ardb.rocksdb.conf";
        return configSource.get(CONFIG_ARDB_ROCKSDB_CONF, DEFAULT_ARDB_ROCKSDB_CONF);
    }

    @Override
    public String getArdbRocksDBInitStart() {
        final String DEFAULT_ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
        final String CONFIG_ARDB_ROCKSDB_START_SCRIPT = DYNOMITEMANAGER_PRE + ".ardb.rocksdb.init.start";
        return configSource.get(CONFIG_ARDB_ROCKSDB_START_SCRIPT, DEFAULT_ARDB_ROCKSDB_START_SCRIPT);
    }

    @Override
    public String getArdbRocksDBInitStop() {
        final String DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT = "/apps/ardb/bin/kill_ardb.sh";
        final String CONFIG_ARDB_ROCKSDB_STOP_SCRIPT = DYNOMITEMANAGER_PRE + ".ardb.rocksdb.init.stop";
        return configSource.get(CONFIG_ARDB_ROCKSDB_STOP_SCRIPT, DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT);
    }
}
