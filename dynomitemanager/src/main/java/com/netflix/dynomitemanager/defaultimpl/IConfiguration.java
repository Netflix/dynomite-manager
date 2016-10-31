/**
 * Copyright 2014 Netflix, Inc.
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

/**
 * Dynomite Manager configuration.
 */
public interface IConfiguration {

    public void initialize();

    /**
     * @return Path to the home dir of target application
     */
    public String getAppHome();

    /**
     * Get the cluster name that is saved in tokens.appId in Cassandra. Cluster name is used to group Dynomite nodes
     * that are part of the same cluster.
     * @return the cluster name
     */
    public String getAppName();

    /**
     * @return Zone (or zone for AWS)
     */
    public String getZone();

    /**
     * @return List of all RAC used for the cluster
     */
    public List<String> getZones();

    /**
     * @return Local hostname
     */
    public String getHostname();

    /**
     * @return Get instance name (for AWS)
     */
    public String getInstanceName();

    /**
     * Get the data center (AWS region).
     *
     * @return the data center (AWS region)
     */
    public String getDataCenter();

    // public void setRegion(String region);

    /**
     * Get the rack (AWS AZ).
     *
     * @return the rack (AWS AZ)
     */
    public String getRack();

    /**
     * @return Get the cross account rack if in dual account mode
     */
    public String getCrossAccountRack();

    public List<String> getRacks();

    /**
     * Amazon specific setting to query ASG Membership
     */
    public String getASGName();

    /**
     * Get the AWS Security Group (SG) assigned to the Dynomite cluster nodes.
     * @return the AWS Security Group
     */
    public String getACLGroupName();

    /**
     * @return Get host IP
     */
    public String getHostIP();

    /**
     * Get the Cassandra cluster name for the topology database (i.e. the database that stores the complete Dynomite
     * cluster topology).
     * @return the Cassandra cluster name for the topology database
     */
    public String getBootClusterName();

    /**
     * @return Process Name
     */
    public String getProcessName();

    public int getPeerListenerPort();

    public int getSecuredPeerListenerPort();

    public int getListenerPort();

    public String getYamlLocation();

    public boolean getAutoEjectHosts();

    public String getDistribution();

    public String getDynListenPort();

    public int getGossipInterval();

    public String getHash();

    public String getClientListenPort();

    public boolean getPreconnect();

    public int getServerRetryTimeout();

    public int getTimeout();

    public String getTokens();

    public boolean isMultiRegionedCluster();

    public String getSecuredOption();

    public boolean isWarmBootstrap();

    public boolean isForceWarm();

    public boolean isHealthCheckEnable();

    public int getAllowableBytesSyncDiff();

    public int getMaxTimeToBootstrap();

    /**
     * @return the max percentage of system memory to be allocated to the
     *         Dynomite fronted data store.
     */
    public int getStorageMemPercent();

    // VPC
    public boolean isVpc();

    /**
     * @return the VPC id of the running instance.
     */
    public String getVpcId();

    /*
     * @return the Amazon Resource Name (ARN) for EC2 classic.
     */
    public String getClassicAWSRoleAssumptionArn();

    /*
     * @return the Amazon Resource Name (ARN) for VPC.
     */
    public String getVpcAWSRoleAssumptionArn();

    /*
     * @return cross-account deployments
     */
    public boolean isDualAccount();

    // Backup and Restore

    public String getBucketName();

    public String getBackupLocation();

    public boolean isBackupEnabled();

    public boolean isRestoreEnabled();

    public String getBackupSchedule();

    public int getBackupHour();

    public String getRestoreDate();

    // Cassandra
    // =========
    // Cassandra can be used to store the complete Dynomite cluster topology.

    public String getCassandraKeyspaceName();

    public int getCassandraThriftPortForAstyanax();

    /**
     * Get a comma separated list of Cassandra hostnames.
     * @return a comma separated list of Cassandra hostnames or IP address
     */
    public String getCassandraHostNames();

    public boolean isEurekaHostSupplierEnabled();

    // Redis
    // =====

    /**
     * Get the full path to the redis.conf configuration file. Netflix:
     * /apps/nfredis/conf/redis.conf DynomiteDB: /etc/dynomitedb/redis.conf
     *
     * @return the {@link String} full path to the redis.conf configuration file
     */
    public String getRedisConf();

    /**
     * Get the full path to the Redis init start script, including any
     * arguments.
     *
     * @return the full path of the Redis init start script
     */
    public String getRedisInitStart();

    /**
     * Get the full path to the Redis init stop script, including any arguments.
     *
     * @return the full path of the Redis init stop script
     */
    public String getRedisInitStop();

    /**
     * Determines whether or not Redis will save data to disk.
     *
     * @return true if Redis should persist in-memory data to disk or false if
     *         Redis should only store data in-memory
     */
    public boolean isRedisPersistenceEnabled();

    /**
     * Get the full path to the directory where Redis stores its AOF or RDB data
     * files.
     *
     * @return the full path to the directory where Redis stores its data files
     */
    public String getRedisDataDir();

    /**
     * Checks if Redis append-only file (AOF) persistence is enabled.
     *
     * @return true to indicate that AOF persistence is enabled or false to
     *         indicate that RDB persistence is enabled
     */
    public boolean isRedisAofEnabled();

    /**
     * Get the type of Redis compatible (RESP) backend server.
     *
     * @return RESP backend server (redis, ardb-rocksdb)
     */
    public String getRedisCompatibleEngine();

    // ARDB RocksDB
    // ============

    /**
     * Get the full path to the rocksdb.conf configuration file. Netflix:
     * /apps/ardb/conf/rocksdb.conf DynomiteDB: /etc/dynomitedb/rocksdb.conf
     *
     * @return the {@link String} full path to the rocksdb.conf configuration
     *         file
     */
    public String getArdbRocksDBConf();

    /**
     * Get the full path to the ARDB RocksDB init start script, including any
     * arguments.
     *
     * @return the full path of the ARDB RocksDB init start script
     */
    public String getArdbRocksDBInitStart();

    /**
     * Get the full path to the ARDB RocksDB init stop script, including any
     * arguments.
     *
     * @return the full path of the ARDB RocksDB init stop script
     */
    public String getArdbRocksDBInitStop();

    public int getWriteBufferSize();

    public int getMaxWriteBufferNumber();

    public int getMinWriteBufferToMerge();

}
