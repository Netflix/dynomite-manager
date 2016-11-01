/**
 * Copyright 2016 Netflix, Inc. <p/> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p/>
 * http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.netflix.dynomitemanager.identity.test;

import java.util.List;

import com.netflix.dynomitemanager.defaultimpl.IConfiguration;

/**
 * IConfiguration implementation for tests.
 *
 * @author diegopacheco
 * @author ipapapa
 * @author akbarahmed
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
	public boolean isRedisPersistenceEnabled() {
		return false;
	}

	@Override
	public boolean isMultiRegionedCluster() {
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
	public boolean isRedisAofEnabled() {
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
	public String getVpcId() {
		return null;
	}

	@Override
	public String getTokens() {
		return null;
	}

	@Override
	public int getStorageMemPercent() {
		return 0;
	}

	@Override
	public String getRestoreDate() {
		return null;
	}

	@Override
	public String getDataCenter() {
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
	public boolean getPreconnect() {
		return false;
	}

	@Override
	public String getRedisDataDir() {
		return null;
	}

	@Override
	public int getMaxTimeToBootstrap() {
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
	public String getCassandraHostNames() {
		return "localhost";
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
	public String getAppName() {
		return "DynomiteManagerTestApp";
	}

	@Override
	public int getAllowableBytesSyncDiff() {
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

	@Override
	public String getCrossAccountRack() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRedisCompatibleEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRedisConf() {
		return null;
	}

	@Override
	public String getRedisInitStart() {
		return null;
	}

	@Override
	public String getRedisInitStop() {
		return null;
	}

	// ARDB RocksDB
	// ============

	@Override
	public String getArdbRocksDBConf() {
		return null;
	}

	@Override
	public String getArdbRocksDBInitStart() {
		return null;
	}

	@Override
	public String getArdbRocksDBInitStop() {
		return null;
	}

	@Override
	public int getWriteBufferSize() {
	    return 0;
	}

	@Override
	public int getMaxWriteBufferNumber() {
	    return 0;
	}

	@Override
	public int getMinWriteBufferToMerge() {
	    return 0;
	}

}
