package com.netflix.dynomitemanager.identity;

/**
 * 
 * This class make sure instance identify is always generated in the same way. 
 * 
 * @author diegopacheco
 *
 */
public class InstanceIdentityUniqueGenerator {
	
	public static String createUniqueRackName(String rack,String instanceId){
		return "rack:_" + rack + "_InsID:_" + instanceId;
	}
	
}
