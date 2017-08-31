package com.netflix.nfsidecar.config;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "florida.config")
public interface CassCommonConfig {

    /**
     * @return Bootstrap cluster name (depends on another cass cluster)
     */
    @DefaultValue("cass_turtle")
    @PropertyName(name = "dyno.sidecore.clusterName")
    public String getCassandraClusterName();

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
    public int getCassandraThriftPort();

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

}
