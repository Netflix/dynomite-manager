/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.identity.test;

import java.util.List;

import com.netflix.dynomitemanager.sidecore.IConfiguration;

/**
 * IConfiguration implementation for tests.
 * 
 * @author diegopacheco
 * @author ipapapa
 *
 */
public class SimpleTestConfiguration implements IConfiguration {

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
		return "localhost";
	}

	@Override
	public int getClusterType() {
		return 0; //DynomitemanagerConfiguration.DYNO_REDIS;
	}

	@Override
	public String getClientListenPort() {
		return null;
	}

	@Override
	public int getCassandraThriftPortForAstyanax() {
		return 9911;
	}

	@Override
	public String getCassandraKeyspaceName() {
		return "dyno_bootstrap";
	}

	@Override
	public String getBucketName() {
		return null;
	}

	@Override
	public String getBootClusterName() {
		return "DynomiteManagerClusterTest";
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
		return "DynomiteManagerTestApp";
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

	@Override
	public boolean isDualAccount() {
		return false;
	}

	@Override
	public boolean isForceWarm() {
		return false;
	}

}
