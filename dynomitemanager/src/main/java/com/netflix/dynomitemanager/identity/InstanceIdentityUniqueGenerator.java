package com.netflix.dynomitemanager.identity;

/**
 * 
 * This class make sure instance identify is always generated in the same way. 
 * 
 * @author diegopacheco
 *
 */
public class InstanceIdentityUniqueGenerator {
	
	public static String createUniqueID(Integer slotToken,String instanceId){
		return createUniqueID(slotToken.toString(),instanceId);
	}
	
	public static String createUniqueID(String slotToken,String instanceId){
		return "slotToken_" + slotToken + "_InsID_" + instanceId;
	}
	
}
