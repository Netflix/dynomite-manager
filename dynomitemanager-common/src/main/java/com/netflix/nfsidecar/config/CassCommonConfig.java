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
package com.netflix.nfsidecar.config;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "dbsidecar.cass")
public interface CassCommonConfig {

    /**
     * @return Bootstrap cluster name (depends on another cass cluster)
     */
    @DefaultValue("cass_turtle")
    @PropertyName(name = "dyno.sidecore.clusterName")
    public String getCassandraClusterName();

    /**
     * @return Name to use for Cassandra's "local datacenter" value
     */
    @PropertyName(name = "dyno.sidecore.datacenterName")
    public String getCassandraDatacenterName();

    /**
     * @return if Eureka is used to find the bootstrap cluster
     */
    @DefaultValue("false")
    @PropertyName(name = "dyno.sidecore.eureka.enabled")
    public boolean isEurekaHostsSupplierEnabled();

    /**
     * @return the port that the bootstrap cluster can be contacted
     */
    @DefaultValue("7102")
    @PropertyName(name = "dyno.sidecore.port")
    public int getCassandraPort();

    @DefaultValue("127.0.0.1")
    @PropertyName(name = "dyno.sidecore.seeds")
    public String getCassandraSeeds();

    /**
     * Get the name of the keyspace that stores tokens for the Dynomite cluster.
     *
     * @return the keyspace name
     */
    @DefaultValue("dyno_bootstrap")
    @PropertyName(name = "metadata.keyspace")
    public String getCassandraKeyspaceName();

    /**
     * @return the refresh interval in msecs for getting the tokens
     * 0 value means, do not cache the tokens. Every query to Dynomite-manager
     * to get tokens will be forwarded to the token store
     */
    @DefaultValue("0")
    @PropertyName(name = "dyno.sidecore.tokenRefreshInterval")
    public long getTokenRefreshInterval();

}
