package com.netflix.dynomitemanager.monitoring.test;

import java.util.List;

import com.netflix.dynomitemanager.sidecore.IConfiguration;

/**
 * Blanck IConfiguration class used for tests.
 * 
 * @author diegopacheco
 *
 */
public class BlankConfiguration implements IConfiguration {

	@Override
	public boolean isWarmBootstrap() {
		return false;
	}

	@Override
	public boolean isVpc() {
		return false;
	}

	@Override
	public boolean isRestoreEnabled() {
		return false;
	}

	@Override
	public boolean isPersistenceEnabled() {
		return false;
	}

	@Override
	public boolean isMultiRegionedCluster() {
		return false;
	}

	@Override
	public boolean isHealthCheckEnable() {
		return false;
	}

	@Override
	public boolean isEurekaHostSupplierEnabled() {
		return false;
	}

	@Override
	public boolean isBackupEnabled() {
		return false;
	}

	@Override
	public boolean isAof() {
		return false;
	}

	@Override
	public void initialize() {
	}

	@Override
	public List<String> getZones() {
		return null;
	}

	@Override
	public String getZone() {
		return null;
	}

	@Override
	public String getYamlLocation() {
		return null;
	}

	@Override
	public String getWriteConsistency() {
		return null;
	}

	@Override
	public String getVpcId() {
		return null;
	}

	@Override
	public String getTokens() {
		return null;
	}

	@Override
	public int getTimeout() {
		return 0;
	}

	@Override
	public String getStorageStopScript() {
		return null;
	}

	@Override
	public String getStorageStartupScript() {
		return null;
	}

	@Override
	public int getStorageMemPercent() {
		return 0;
	}

	@Override
	public int getServerRetryTimeout() {
		return 0;
	}

	@Override
	public String getSeedProviderName() {
		return null;
	}

	@Override
	public int getSecuredPeerListenerPort() {
		return 0;
	}

	@Override
	public String getSecuredOption() {
		return null;
	}

	@Override
	public String getRestoreDate() {
		return null;
	}

	@Override
	public String getRegion() {
		return null;
	}

	@Override
	public String getReadConsistency() {
		return null;
	}

	@Override
	public List<String> getRacks() {
		return null;
	}

	@Override
	public String getRack() {
		return null;
	}

	@Override
	public String getProcessName() {
		return null;
	}

	@Override
	public boolean getPreconnect() {
		return false;
	}

	@Override
	public String getPersistenceLocation() {
		return null;
	}

	@Override
	public int getPeerListenerPort() {
		return 0;
	}

	@Override
	public String getMetadataKeyspace() {
		return null;
	}

	@Override
	public int getMbufSize() {
		return 0;
	}

	@Override
	public int getMaxTimeToBootstrap() {
		return 0;
	}

	@Override
	public int getListenerPort() {
		return 0;
	}

	@Override
	public String getInstanceName() {
		return null;
	}

	@Override
	public String getHostname() {
		return null;
	}

	@Override
	public String getHostIP() {
		return null;
	}

	@Override
	public String getHash() {
		return null;
	}

	@Override
	public int getGossipInterval() {
		return 0;
	}

	@Override
	public String getDynListenPort() {
		return null;
	}

	@Override
	public String getDistribution() {
		return null;
	}

	@Override
	public String getCommaSeparatedCassandraHostNames() {
		return null;
	}

	@Override
	public int getClusterType() {
		return 0;
	}

	@Override
	public String getClientListenPort() {
		return null;
	}

	@Override
	public int getCassandraThriftPortForAstyanax() {
		return 0;
	}

	@Override
	public String getCassandraKeyspaceName() {
		return null;
	}

	@Override
	public String getBucketName() {
		return null;
	}

	@Override
	public String getBootClusterName() {
		return null;
	}

	@Override
	public String getBackupSchedule() {
		return null;
	}

	@Override
	public String getBackupLocation() {
		return null;
	}

	@Override
	public int getBackupHour() {
		return 0;
	}

	@Override
	public boolean getAutoEjectHosts() {
		return false;
	}

	@Override
	public String getAppStopScript() {
		return null;
	}

	@Override
	public String getAppStartupScript() {
		return null;
	}

	@Override
	public String getAppName() {
		return null;
	}

	@Override
	public String getAppHome() {
		return null;
	}

	@Override
	public int getAllowableBytesSyncDiff() {
		return 0;
	}

	@Override
	public int getAllocatedMessages() {
		return 0;
	}

	@Override
	public String getASGName() {
		return null;
	}

	@Override
	public String getACLGroupName() {
		return null;
	}
	
	@Override
	public String getClassicAWSRoleAssumptionArn() {
		return null;
	}
	
	@Override
	public String getVpcAWSRoleAssumptionArn() {
		return null;
	}


}
