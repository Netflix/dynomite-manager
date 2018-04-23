/**
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.config;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "florida")
public interface FloridaConfig {

    /**
     * Get the full path to Dynomite's installation directory.
     *
     * @return full path to the Dynomite installation directory
     */
    @DefaultValue("/apps/dynomite")
    @PropertyName(name = "dyno.home")
    public String getDynomiteInstallDir();

    /**
     * @return Path to target application startup script
     */
    @DefaultValue("/apps/dynomite/bin/launch_dynomite.sh")
    @PropertyName(name = "dyno.startscript")
    public String getDynomiteStartScript();

    /**
     * @return Path to target application stop sript
     */
    @DefaultValue("/apps/dynomite/bin/kill_dynomite.sh")
    @PropertyName(name = "dyno.stopscript")
    public String getDynomiteStopScript();

    /**
     * @return Cluster name if the environment variable for cluster name does
     *         not exist
     */
    @DefaultValue("dynomite_demo1")
    @PropertyName(name = "dyno.clustername")
    public String getDynomiteClusterName();

    /**
     * YAML file to bootstrap Dynomite
     * 
     * @return
     */
    @DefaultValue("/apps/dynomite/conf/dynomite.yml")
    public String getDynomiteYaml();

    /**
     * @return Get the name of seed provider
     */
    @DefaultValue("florida_provider")
    @PropertyName(name = "dyno.seed.provider")
    public String getDynomiteSeedProvider();

    /**
     * Get the Dynomite process name.
     *
     * @return the Dynomite process name
     */
    @DefaultValue("dynomite")
    public String getDynomiteProcessName();

    /**
     * Get the read consistency level.
     *
     * @return the read consistency level
     */
    @DefaultValue("DC_ONE")
    @PropertyName(name = "dyno.read.consistency")
    public String getDynomiteReadConsistency();

    /**
     * Get the write consistency level.
     *
     * @return the write consistency level
     */
    @DefaultValue("DC_ONE")
    @PropertyName(name = "dyno.write.consistency")
    public String getDynomiteWriteConsistency();

    @DefaultValue("8101")
    @PropertyName(name = "dyno.secured.peer.port")
    public int getSecuredPeerListenerPort();

    @DefaultValue("8102")
    public int getDynomiteClientPort();

    @DefaultValue("127.0.0.1")
    public String getDynomiteLocalAddress();

    /**
     * Dynomite now supports multiple connections to datastore and peer. These
     * are the set of properties for each of them
     * 
     * @return the peer-to-peer port used for intra-cluster communication
     */
    // we need this for backward compatibility.
    @DefaultValue("false")
    @PropertyName(name = "dyno.connections.pool.enable")
    public boolean getConnectionPoolEnabled();

    @DefaultValue("1")
    @PropertyName(name = "dyno.connections.storage")
    public int getDatastoreConnections();

    @DefaultValue("1")
    @PropertyName(name = "dyno.connections.peer.local")
    public int getLocalPeerConnections();

    @DefaultValue("1")
    @PropertyName(name = "dyno.connections.peer.remote")
    public int getRemotePeerConnections();

    /**
     * Dynomite support of hashtags Link:
     * https://github.com/Netflix/dynomite/blob/dev/notes/recommendation.md#hash-tags
     */
    @DefaultValue("")
    @PropertyName(name = "dyno.hashtag")
    public String getDynomiteHashtag();

    /**
     * Determine if Dynomite should auto-eject nodes from the cluster.
     *
     * @return true if Dynomite should auto-ejects hosts, false if not
     */
    @DefaultValue("true")
    public boolean getDynomiteAutoEjectHosts();

    @DefaultValue("vnode")
    public String getDistribution();

    /**
     * Get the Dynomite gossip interval which is the amount of time (in ms) that
     * Dynomite should wait between gossip rounds.
     *
     * @return the amount of time in ms to wait between gossip rounds
     */
    @DefaultValue("10000")
    @PropertyName(name = "dyno.gossip.interval")
    public int getDynomiteGossipInterval();

    /**
     * Get the hash algorithm that Dynomite uses to hash the data's key.
     *
     * @return the hash algorithm used to hash the data key
     */
    @DefaultValue("murmur")
    @PropertyName(name = "dyno.tokens.hash")
    public String getDynomiteHashAlgorithm();

    /**
     * Should Dynomite preconnect to the backend storage engine.
     *
     * @return true if Dynomite should preconnect to the backend storage engine,
     *         false if it should not preconnect
     */
    @DefaultValue("true")
    @PropertyName(name = "dyno.connections.preconnect")
    public boolean getDynomiteStoragePreconnect();

    @DefaultValue("30000")
    public int getServerRetryTimeout();

    @DefaultValue("5000")
    @PropertyName(name = "dyno.request.timeout")
    public int getTimeout();

    /**
     * Determine if Dynomite is configured as a multi-DC (data center) cluster).
     *
     * @return true if the Dynomite cluster is running across multiple DCs
     */
    @DefaultValue("true")
    @PropertyName(name = "dyno.multiregion")
    public boolean isDynomiteMultiDC();

    /**
     * Get the intra-cluster (i.e. node-to-node) security option. Maps to the
     * secure_server_option property in dynomite.yaml.
     *
     * @return the intra-cluster security option
     */
    @DefaultValue("datacenter")
    @PropertyName(name = "dyno.secured.option")
    public String getDynomiteIntraClusterSecurity();

    /**
     * If warm up is enabled for the cluster
     * 
     * @return enabled/disabled
     */
    @DefaultValue("true")
    @PropertyName(name = "dyno.warm.bootstrap")
    public boolean isWarmBootstrap();

    /**
     * Enforcing the warm up
     * 
     * @return enabled/disabled
     */
    @DefaultValue("false")
    @PropertyName(name = "dyno.warm.force")
    public boolean isForceWarm();

    @DefaultValue("true")
    @PropertyName(name = "dyno.healthcheck.enable")
    public boolean isHealthCheckEnable();

    @DefaultValue("1000000")
    @PropertyName(name = "dyno.warm.bytes.sync.diff")
    public int getAllowableBytesSyncDiff();

    @DefaultValue("1200000")
    @PropertyName(name = "dyno.warm.msec.bootstraptime")
    public int getMaxTimeToBootstrap();

    /**
     * The max percentage of system memory to be allocated to the Dynomite
     * fronted data store.
     */
    @DefaultValue("85")
    @PropertyName(name = "dyno.storage.mem.pct.int")
    public int getStorageMaxMemoryPercent();

    /**
     * Get the size (in bytes) of Dynomite's memory buffer (mbuf).
     *
     * @return size of Dynomite mbuf in bytes
     */
    @DefaultValue("16384")
    @PropertyName(name = "dyno.mbuf.size")
    public int getDynomiteMBufSize();

    /**
     * Get the maximum number of messages that Dynomite will hold in queue.
     * Default is 0, such that we can let Florida automate the value based on
     * the instance type.
     *
     * @return the maximum number of messages that Dynomite will allocate
     */
    @DefaultValue("0")
    @PropertyName(name = "dyno.allocated.messages")
    public int getDynomiteMaxAllocatedMessages();

    // VPC
    @DefaultValue("true")
    public boolean isVpc();

    // Persistence

    @DefaultValue("/mnt/data/nfredis")
    @PropertyName(name = "dyno.persistence.directory")
    public String getPersistenceLocation();

    @DefaultValue("false")
    @PropertyName(name = "dyno.persistence.enabled")
    public boolean isPersistenceEnabled();

    @DefaultValue("aof")
    @PropertyName(name = "dyno.persistence.type")
    public String persistenceType();

    // Storage engine: ARDB with RocksDB
    // =================================

    /**
     * Compaction strategy for RocksDB. RocksDB allows for optimized compaction
     * strategies: OptimizeLevelStyleCompaction,
     * OptimizeUniversalStyleCompaction or none.
     * 
     * @return the compaction strategy
     */
    @DefaultValue("none")
    @PropertyName(name = "dyno.ardb.rocksdb.compactionStrategy")
    public String getRocksDBCompactionStrategy();

    @DefaultValue("256")
    @PropertyName(name = "dyno.ardb.rocksdb.writebuffermb")
    public int getRocksDBWriteBufferSize();

    /**
     * Get the maximum number of memtables used by RocksDB. This number includes
     * both active and immutable memtables.
     *
     * @return the maximum number of memtables
     */
    @DefaultValue("16")
    @PropertyName(name = "dyno.ardb.rocksdb.maxwritebuffernumber")
    public int getRocksDBMaxWriteBufferNumber();

    /**
     * Loglevel
     * 
     * @return the loglevel to set for RocksDB
     */
    @DefaultValue("info")
    @PropertyName(name = "dyno.ardb.loglevel")
    public String getArdbLoglevel();

    /**
     * Get the minimum number of memtables to be merged before flushing data to
     * persistent storage.
     *
     * @return the minimum number of memtables that must exist before a flush
     *         occurs
     */
    @DefaultValue("4")
    @PropertyName(name = "dyno.ardb.rocksdb.minwritebuffernametomerge")
    public int getRocksDBMinWriteBuffersToMerge();

    @DefaultValue("redis")
    @PropertyName(name = "dyno.redis.compatible.engine")
    public String getRedisCompatibleEngine();

    @DefaultValue("")
    @PropertyName(name = "redis.pubsub.keyspacevents")
    public String getKeySpaceEvents();

    @DefaultValue("")
    @PropertyName(name = "redis.unixpath")
    public String getRedisUnixPath();

}
