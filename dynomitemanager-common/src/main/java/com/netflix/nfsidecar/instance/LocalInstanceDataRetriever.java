package com.netflix.nfsidecar.instance;

/**
 * Looks at local (system) properties for metadata about the running 'instance'.
 * Typically, this is used for locally-deployed testing.
 *
 * @author jason brown
 */
public class LocalInstanceDataRetriever implements InstanceDataRetriever
{
    private static final String PREFIX = "florida.localInstance.";

    public String getRac()
    {
        return System.getProperty(PREFIX + "availabilityZone", "");
    }

    public String getPublicHostname()
    {
        return System.getProperty(PREFIX + "publicHostname", "");
    }

    public String getPublicIP()
    {
        return System.getProperty(PREFIX + "publicIp", "");
    }

    public String getInstanceId()
    {
        return System.getProperty(PREFIX + "instanceId", "");
    }

    public String getInstanceType()
    {
        return System.getProperty(PREFIX + "instanceType", "");
    }
    
	public String getMac() {
        return System.getProperty(PREFIX + "instanceMac", "");
	}

	@Override
	public String getVpcId() {
		throw new UnsupportedOperationException("Not applicable as running instance is in classic environment");
	}
    
    
}
