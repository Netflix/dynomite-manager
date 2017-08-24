package com.netflix.nfsidecar.identity;

/*
 * A means to determine the environment for the running instance
 */
public interface InstanceEnvIdentity {
	/*
	 * @return true if running instance is in "classic", false otherwise.
	 */
	public Boolean isClassic();
	/*
	 * @return true if running instance is in VPC, under your default AWS account, false otherwise.
	 */
	public Boolean isDefaultVpc();
	/*
	 * @return true if running instance is in VPC, under a specific AWS account, false otherwise.
	 */
	public Boolean isNonDefaultVpc();
	
	public static enum InstanceEnvironent {
		CLASSIC, DEFAULT_VPC, NONDEFAULT_VPC
	};
}
