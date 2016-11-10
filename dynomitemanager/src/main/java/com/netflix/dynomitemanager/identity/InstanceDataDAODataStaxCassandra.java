package com.netflix.dynomitemanager.identity;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataStax Driver Cassandra Implementation
 * 
 * @author diegopacheco
 * @since 11/09/2016
 *
 */
public class InstanceDataDAODataStaxCassandra implements InstanceData {
	
	private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAODataStaxCassandra.class);
	
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
