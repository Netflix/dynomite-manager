package com.netflix.dynomitemanager.sidecore.storage;

import java.io.IOException;

public class MemcachedStorageProxy implements StorageProxy {

    private static final String DYNO_MEMCACHED = "memcached";
    private static final int MEMCACHE_PORT = 11211;
    private static final String MEMCACHE_ADDRESS = "127.0.0.1";
    
    private final String DEFAULT_MEMCACHED_START_SCRIPT = "/apps/memcached/bin/memcached";
    private final String DEFAULT_MEMCACHED_STOP_SCRIPT = "/usr/bin/pkill memcached";


    @Override
    public String getEngine() {
	return DYNO_MEMCACHED;
    }   
    
    @Override
    public int getEngineNumber() {
	return 1;
    }
    
    @Override
    public boolean isAlive() {
	return false;
    }
    
    @Override
    public long getUptime() {
	return 0;
    }

    @Override
    public Bootstrap warmUpStorage(String[] peers) {
	return Bootstrap.IN_SYNC_SUCCESS;
    }

    @Override
    public boolean resetStorage() {
	return true;
    }

    @Override
    public boolean takeSnapshot() {
	return false;
    }

    @Override
    public boolean loadingData() {
	return false;
    }

    @Override
    public void stopPeerSync() {

    }

    @Override
    public void updateConfiguration() throws IOException {
	// TODO Auto-generated method stub
	
    }

    @Override
    public String getStartupScript() {
	return DEFAULT_MEMCACHED_START_SCRIPT;
    }

    @Override
    public String getStopScript() {
	return DEFAULT_MEMCACHED_STOP_SCRIPT;
    }

    @Override
    public String getIpAddress() {
	return MEMCACHE_ADDRESS;
	
    }

    @Override
    public int getPort() {
	return MEMCACHE_PORT;	
    }

    @Override
    public long getStoreMaxMem() {
	return 0;
    }

    @Override
    public long getTotalAvailableSystemMemory() {
	return 0;
    }

}
