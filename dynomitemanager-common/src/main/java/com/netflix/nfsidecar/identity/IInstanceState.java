package com.netflix.nfsidecar.identity;

public interface IInstanceState {

    public boolean isSideCarProcessAlive();

    public boolean isBootstrapping();

    public boolean getYmlWritten();

    public void setYmlWritten(boolean b);
    
    public void setStorageProxyAlive(boolean isStorageProxyAlive);

}