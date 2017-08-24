package com.netflix.florida.defaultimpl.test;

import com.netflix.florida.instance.InstanceDataRetriever;

public class FakeInstanceDataRetriever implements InstanceDataRetriever {

    @Override
    public String getRac() {       
        return "us-east-1";
    }

    @Override
    public String getPublicHostname() {
        return "dynomite";
    }

    @Override
    public String getPublicIP() {
        return "0.0.0.0";
    }

    @Override
    public String getInstanceId() {
        return "i-abcdefg";
    }

    @Override
    public String getInstanceType() {
        return "r3.2xlarge";
    }

    @Override
    public String getMac() {
        return "00:00:00:00:00";
    }

    @Override
    public String getVpcId() {
        return "no";
    }

}
