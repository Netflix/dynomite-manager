package com.netflix.dynomitemanager.identity;

public class DefaultVpcInstanceEnvIdentity implements InstanceEnvIdentity {
	
	public DefaultVpcInstanceEnvIdentity() {}
	
	@Override
	public Boolean isClassic() {
		return false;
	}
	
	@Override
	public Boolean isDefaultVpc() {
		return true;
	}
	
	@Override
	public Boolean isNonDefaultVpc() {
		return false;
	}
	
}
