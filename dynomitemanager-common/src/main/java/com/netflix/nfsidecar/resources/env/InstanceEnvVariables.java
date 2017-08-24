package com.netflix.nfsidecar.resources.env;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.nfsidecar.config.CommonConfig;

public class InstanceEnvVariables implements IEnvVariables {

    private static final Logger logger = LoggerFactory.getLogger(InstanceEnvVariables.class);

    CommonConfig config;

    @Inject
    public InstanceEnvVariables(CommonConfig config) {
        this.config = config;
    }

    @Override
    public String getDynomiteClusterName() {
        String clusterName = System.getenv("NETFLIX_APP");
/*        if (StringUtils.isBlank(clusterName)) {
            logger.warn("Cluster name variable not defined. Falling back to FP " + config.getDynomiteClusterName());
            clusterName = config.getDynomiteClusterName();
        }
        */
        return clusterName;
    }

    @Override
    public String getRegion() {
        String region = System.getenv("EC2_REGION");
        if (StringUtils.isBlank(region)) {
            logger.warn("Region environment variable not defined. Falling back to " + config.getRegion());
            region = config.getRegion();
        }
        return region;
    }

    @Override
    public String getRack() {
        String rack = System.getenv("NETFLIX_AUTO_SCALE_GROUP");
        if (StringUtils.isBlank(rack)) {
            logger.error("Rack environment variable not defined. Falling back to " + config.getRack());
            rack = config.getRack();
        }
        return rack;
    }

}
