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
package com.netflix.dynomitemanager.defaultimpl.test;

import java.io.IOException;

import com.netflix.dynomitemanager.sidecore.storage.Bootstrap;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;

public class FakeStorageProxy implements IStorageProxy {

    @Override
    public boolean isAlive() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public long getUptime() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public Bootstrap warmUpStorage(String[] peers) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public boolean resetStorage() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean takeSnapshot() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean loadingData() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public void stopPeerSync() {
	// TODO Auto-generated method stub

    }

    @Override
    public String getEngine() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public int getEngineNumber() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public void updateConfiguration() throws IOException {
	// TODO Auto-generated method stub

    }

    @Override
    public String getStartupScript() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public String getStopScript() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public String getIpAddress() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public int getPort() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public long getStoreMaxMem() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public long getTotalAvailableSystemMemory() {
	// TODO Auto-generated method stub
	return 0;
    }

}
