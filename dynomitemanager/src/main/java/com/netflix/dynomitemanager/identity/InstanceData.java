package com.netflix.dynomitemanager.identity;

import java.util.Set;

/**
 * This is the main interface around Persistence
 * 
 * @author diegopacheco
 * @since 11/09/2016
 *
 */
public interface InstanceData {
	
	public void createInstanceEntry(AppsInstance instance);
	
	public void deleteInstanceEntry(AppsInstance instance);
	
	public AppsInstance getInstance(String app, String rack, int id);
	
	public Set<AppsInstance> getLocalDCInstances(String app, String region);
	
	public Set<AppsInstance> getAllInstances(String app);
	
	public String findKey(String app, String id, String location, String datacenter);
	
}
