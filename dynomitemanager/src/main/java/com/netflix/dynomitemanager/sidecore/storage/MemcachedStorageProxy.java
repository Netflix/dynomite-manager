/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.storage;

import java.io.IOException;

public class MemcachedStorageProxy implements IStorageProxy {

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

}
