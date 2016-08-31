/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore;

import java.util.List;

public interface IConfiguration {

	public void initialize();

	/**
	 * @return Path to the home dir of target application
	 */
	public String getAppHome();


	/**
	 * @return Path to target application startup script
	 */
	public String getAppStartupScript();


	/**
	 * @return Path to target application stop script
	 */
	public String getAppStopScript();

	/**
	 * @return Script that starts the storage layer
	 */
	public String getStorageStartupScript();


	/*
	 *
	 */
	public String getStorageStopScript();


	/**
	 * @return Cluster name
	 */
	public String getAppName();

	/**
	 * @return Zone (or zone for AWS)
	 */
	public String getZone();

	/**
	 * @return List of all RAC used for the cluster
	 */
	public List<String> getZones();

	/**
	 * @return Local hostmame
	 */
	public String getHostname();

	/**
	 * @return Get instance name (for AWS)
	 */
	public String getInstanceName();

	/**
	 * @return Get the Region name
	 */
	public String getRegion();

	//public void setRegion(String region);

	/**
	 * @return Get the Data Center name (or region for AWS)
	 */
	public String getRack();

	public List<String> getRacks();


	/**
	 * Amazon specific setting to query ASG Membership
	 */
	public String getASGName();

	/**
	 * Get the security group associated with nodes in this cluster
	 */
	public String getACLGroupName();


	/**
	 * @return Get host IP
	 */
	public String getHostIP();


	/**
	 * @return Bootstrap cluster name (depends on another cass cluster)
	 */
	public String getBootClusterName();

	/**
	 * @return Get the name of seed provider
	 */
	public String getSeedProviderName();

	/**
	 * @return Process Name
	 */
	public String getProcessName();

	public String getReadConsistency();

	public String getWriteConsistency();

	public int getPeerListenerPort();

	public int getSecuredPeerListenerPort();

	public int getListenerPort();

	public String getYamlLocation();


	public boolean getAutoEjectHosts();

	public String getDistribution();

	public String getDynListenPort();

	public int getGossipInterval();

	public String getHash();

	public String getClientListenPort();

	public boolean getPreconnect();

	public int getServerRetryTimeout();

	public int getTimeout();

	public String getTokens();

	public String getMetadataKeyspace();

	public int getClusterType();

	public boolean isMultiRegionedCluster();

	public String getSecuredOption();

	public boolean isWarmBootstrap();

	public boolean isForceWarm();

	public boolean isHealthCheckEnable();

	public int getAllowableBytesSyncDiff();

	public int getMaxTimeToBootstrap();

	/** The max percentage of system memory to be allocated to the Dynomite fronted data store. */
	public int getStorageMemPercent();

	public int getMbufSize();

	public int getAllocatedMessages();


	// VPC
	public boolean isVpc();

	/**
	 * @return the VPC id of the running instance.
	 */
	public String getVpcId();

	/*
	 * @return the Amazon Resource Name (ARN) for EC2 classic.
	 */
	public String getClassicAWSRoleAssumptionArn();

	/*
	 * @return the Amazon Resource Name (ARN) for VPC.
	 */
	public String getVpcAWSRoleAssumptionArn();

	/*
	 * @return cross-account deployments
	 */
	public boolean isDualAccount();


	// Backup and Restore

	public String getBucketName();

	public String getBackupLocation();

	public boolean isBackupEnabled();

	public boolean isRestoreEnabled();

	public String getBackupSchedule();

	public int getBackupHour();

	public String getRestoreDate();

	// Persistence

	public String getPersistenceLocation();

	public boolean isPersistenceEnabled();

	public boolean isAof();

	// Cassandra
	public String getCassandraKeyspaceName();

	public int getCassandraThriftPortForAstyanax();

	public String getCommaSeparatedCassandraHostNames();

	public boolean isEurekaHostSupplierEnabled();

}
