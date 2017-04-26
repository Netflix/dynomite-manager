package com.netflix.dynomitemanager.identity;

/**
 * 
 * This class make sure instance identify is always generated in the same way. 
 * 
 * @author diegopacheco
 *
 */
public class InstanceIdentityUniqueGenerator {
	
	public static String createUniqueID(String instanceId){
		return "InsID_" + instanceId;
	}
	
}
