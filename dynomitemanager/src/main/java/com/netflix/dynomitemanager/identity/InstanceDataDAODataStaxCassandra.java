package com.netflix.dynomitemanager.identity;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.supplier.HostSupplier;

/**
 * DataStax Driver Cassandra Implementation
 * 
 * @author diegopacheco
 * @since 11/09/2016
 *
 */
public class InstanceDataDAODataStaxCassandra implements InstanceData {
	
	private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAODataStaxCassandra.class);
	
	private String CN_ID = "Id";
	private String CN_APPID = "appId";
	private String CN_AZ = "availabilityZone";
	private String CN_DC = "datacenter";
	private String CN_INSTANCEID = "instanceId";
	private String CN_HOSTNAME = "hostname";
	private String CN_EIP = "elasticIP";
	private String CN_TOKEN = "token";
	private String CN_LOCATION = "location";
	private String CN_VOLUME_PREFIX = "ssVolumes";
	private String CN_UPDATETIME = "updatetime";
	private String CF_NAME_TOKENS = "tokens";
	private String CF_NAME_LOCKS = "locks";
	
	private final String BOOT_CLUSTER;
	private final String KS_NAME;
	private Cluster cluster;
	private Session session;
	
	private final IConfiguration config;
	private final HostSupplier hostSupplier;
	
	@Inject
	public InstanceDataDAODataStaxCassandra(IConfiguration config, HostSupplier hostSupplier) {
		this.config = config;
		this.hostSupplier = hostSupplier;
		
		BOOT_CLUSTER = config.getBootClusterName();
		if (BOOT_CLUSTER == null || BOOT_CLUSTER.isEmpty())
			throw new RuntimeException("BootCluster can not be blank. Please use getBootClusterName() property.");

		KS_NAME = config.getCassandraKeyspaceName();
		if (KS_NAME == null || KS_NAME.isEmpty())
			throw new RuntimeException("Cassandra Keyspace can not be blank. Please use getCassandraKeyspaceName() property.");

		if (config.isEurekaHostSupplierEnabled())
			//TODO: SUPORT EUREKA
			;
		else
			initWithNativeDriverWithExternalHostsSupplier();

//		ctx.start();
//		bootKeyspace = ctx.getClient();
	}
	
	private Cluster initWithNativeDriverWithExternalHostsSupplier() {

		logger.info("BOOT_CLUSTER = {}, KS_NAME = {}", BOOT_CLUSTER, KS_NAME);
		cluster = Cluster.builder()
                .addContactPoints(config.getCassandraSeeds().split(",")).withPort(9042)
                .build();
		
//		PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
//		poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL,  8);
//		poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, 2);
		
        this.session = cluster.connect();
        return cluster;
	}
	
	
	@Override
	public void createInstanceEntry(AppsInstance instance) {
	}

	@Override
	public void deleteInstanceEntry(AppsInstance instance) {
	}

	@Override
	public AppsInstance getInstance(String app, String rack, int id) {
		return null;
	}

	@Override
	public Set<AppsInstance> getLocalDCInstances(String app, String region) {
		return null;
	}

	@Override
	public Set<AppsInstance> getAllInstances(String app) {
		return null;
	}

	@Override
	public String findKey(String app, String id, String location, String datacenter) {
		return null;
	}

}
