package com.netflix.nfsidecar.resources.env;

public interface IEnvVariables {

    public String getDynomiteClusterName();
    
    public String getRegion();
    
    public String getRack();
}
