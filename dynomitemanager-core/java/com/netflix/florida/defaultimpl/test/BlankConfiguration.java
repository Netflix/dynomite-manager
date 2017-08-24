package com.netflix.florida.defaultimpl.test;

import java.util.List;

import com.netflix.florida.config.FloridaConfig;

public class BlankConfiguration implements FloridaConfig {

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
    public boolean isDynomiteMultiDC() {
        return false;
    }

    @Override
    public boolean isBackupEnabled() {
        return false;
    }

    @Override
    public String getDynomiteYaml() {
        return null;
    }

    @Override
    public String getDynomiteWriteConsistency() {
        return "DC_SAFE_QUORUM";
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public int getStorageMaxMemoryPercent() {
        return 0;
    }

    @Override
    public int getServerRetryTimeout() {
        return 0;
    }

    @Override
    public String getDynomiteSeedProvider() {
        return null;
    }

    @Override
    public String getDynomiteIntraClusterSecurity() {
        return null;
    }

    @Override
    public String getRestoreDate() {
        return null;
    }

    @Override
    public String getDynomiteReadConsistency() {
        return "DC_SAFE_QUORUM";
    }

    @Override
    public List<String> getRacks() {
        return null;
    }

    @Override
    public String getDynomiteProcessName() {
        return null;
    }

    @Override
    public boolean getDynomiteStoragePreconnect() {
        return false;
    }

    @Override
    public int getStoragePeerPort() {
        return 0;
    }

    @Override
    public int getDynomiteMBufSize() {
        return 0;
    }

    @Override
    public int getMaxTimeToBootstrap() {
        return 0;
    }

    @Override
    public String getDynomiteHashAlgorithm() {
        return null;
    }

    @Override
    public int getDynomiteGossipInterval() {
        return 0;
    }


    @Override
    public String getDistribution() {
        return null;
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
    public String getCassandraClusterName() {
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
    public boolean getDynomiteAutoEjectHosts() {
        return true;
    }

    @Override
    public String getDynomiteStopScript() {
        return null;
    }

    @Override
    public String getDynomiteStartScript() {
        return null;
    }

    @Override
    public String getDynomiteInstallDir() {
        return "/apps/dynomite";
    }

    @Override
    public int getAllowableBytesSyncDiff() {
        return 0;
    }

    @Override
    public int getDynomiteMaxAllocatedMessages() {
        return 0;
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
    public String getRedisCompatibleEngine() {
        return null;
    }

    // ARDB RocksDB
    // ============

    @Override
    public int getWriteBufferSize() {
        return 0;
    }

    @Override
    public int getArdbRocksDBMaxWriteBufferNumber() {
        return 0;
    }

    @Override
    public int getArdbRocksDBMinWriteBuffersToMerge() {
        return 0;
    }

    @Override
    public String getDynomiteClusterName() {
        return null;
    }

    @Override
    public String getRegion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getSecuredPeerListenerPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDynomiteClientPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isHealthCheckEnable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getPersistenceLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPersistenceEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getKeySpaceEvents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String persistenceType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getRack() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDynomiteLocalAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDatastoreConnections() {
        return 1;
    }

    @Override
    public int getLocalPeerConnections() {
       return 1;
    }

    @Override
    public int getRemotePeerConnections() {
       return 1;
    }

    @Override
    public boolean getConnectionPoolEnabled() {
        return false;
    }

    @Override
    public String getRedisUnixPath() {
        return "";
    }
}
