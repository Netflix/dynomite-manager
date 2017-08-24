package com.netflix.florida.defaultimpl.test;

import java.io.IOException;

import com.netflix.florida.sidecore.storage.Bootstrap;
import com.netflix.florida.sidecore.storage.StorageProxy;

public class FakeStorageProxy implements StorageProxy {

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
    public String getUnixPath() {
        return "";
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
