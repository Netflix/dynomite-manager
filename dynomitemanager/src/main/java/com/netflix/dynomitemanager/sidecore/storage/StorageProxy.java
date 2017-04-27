package com.netflix.dynomitemanager.sidecore.storage;

import java.io.IOException;


public interface StorageProxy {

    boolean isAlive();
        
    long getUptime();

    Bootstrap warmUpStorage(String[] peers);

    boolean resetStorage();

    boolean takeSnapshot();

    boolean loadingData();

    void stopPeerSync();
    
    String getEngine();
    
    int getEngineNumber();
    
    void updateConfiguration() throws IOException;
    
    String getStartupScript();
    
    String getStopScript();
    
    String getIpAddress();
    
    int getPort();
    
    long getStoreMaxMem();
    
    long getTotalAvailableSystemMemory();


}