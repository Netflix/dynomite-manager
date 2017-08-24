package com.netflix.nfsidecar.instance;

public interface InstanceDataRetriever
{
    String getRac();
    String getPublicHostname();
    String getPublicIP();
    String getInstanceId();
    String getInstanceType();
    String getMac(); //fetch id of the network interface for running instance
    String getVpcId(); //the id of the vpc for running instance
}
