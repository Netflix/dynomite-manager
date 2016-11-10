package com.netflix.dynomitemanager.identity;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
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

//		if (config.isEurekaHostSupplierEnabled())
//			ctx = initWithThriftDriverWithEurekaHostsSupplier();
//		else
//			ctx = initWithThriftDriverWithExternalHostsSupplier();
//
//		ctx.start();
//		bootKeyspace = ctx.getClient();
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
