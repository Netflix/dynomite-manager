package com.netflix.dynomitemanager.identity;

public class LocalInstanceEnvIdentity implements InstanceEnvIdentity {

	@Override
	public Boolean isClassic() {
		// TODO exemplar local implementation
		return true;
	}

	@Override
	public Boolean isDefaultVpc() {
		// TODO exemplar local implementation
		return false;
	}

	@Override
	public Boolean isNonDefaultVpc() {
		// TODO exemplar local implementation
		return false;
	}

}
