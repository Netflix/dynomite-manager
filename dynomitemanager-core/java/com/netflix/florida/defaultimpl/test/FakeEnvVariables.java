package com.netflix.florida.defaultimpl.test;

import com.netflix.florida.resources.env.IEnvVariables;

public class FakeEnvVariables implements IEnvVariables {

    @Override
    public String getDynomiteClusterName() {
        return "Dynomite";
    }

    @Override
    public String getRegion() {
        return "us-east-1";
    }

    @Override
    public String getRack() {
        return "us-east-1c";
    }

}
