package com.netflix.nfsidecar.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.nfsidecar.utils.SystemUtils;

/**
 * Calls AWS ec2 metadata to get info on the location of the running instance in
 * VPC. Public Hostname will return local-hostname Public IP will return
 * local-ipv4
 */
public class VpcInstanceDataRetriever implements InstanceDataRetriever {
    private static final Logger logger = LoggerFactory.getLogger(VpcInstanceDataRetriever.class);

    public String getRac() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/placement/availability-zone");
    }

    public String getPublicHostname() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-hostname");
    }

    public String getPublicIP() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/public-ipv4");
    }

    public String getInstanceId() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/instance-id");
    }

    public String getInstanceType() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/instance-type");
    }

    @Override
    /*
     * @return id of the network interface for running instance
     */
    public String getMac() {
        return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/network/interfaces/macs/").trim();
    }

    @Override
    /*
     * @return the id of the vpc account for running instance, null if does not
     * exist.
     */
    public String getVpcId() {
        String nacId = getMac();
        if (nacId == null || nacId.isEmpty())
            return null;

        String vpcId = null;
        try {
            vpcId = SystemUtils
                    .getDataFromUrl(
                            "http://169.254.169.254/latest/meta-data/network/interfaces/macs/" + nacId + "vpc-id")
                    .trim();
        } catch (Exception e) {
            logger.info(
                    "Vpc id does not exist for running instance, not fatal as running instance maybe not be in vpc.  Msg: "
                            + e.getLocalizedMessage());
        }

        return vpcId;
    }
}
