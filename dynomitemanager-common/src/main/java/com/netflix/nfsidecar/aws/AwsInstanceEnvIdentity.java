package com.netflix.nfsidecar.aws;

import com.netflix.nfsidecar.identity.InstanceEnvIdentity;
import com.netflix.nfsidecar.instance.InstanceDataRetriever;
import com.netflix.nfsidecar.instance.VpcInstanceDataRetriever;

/**
 * A means to determine if running instance is within classic, default vpc account, or non-default vpc account
 */
public class AwsInstanceEnvIdentity implements InstanceEnvIdentity {

    private Boolean isClassic = false, isDefaultVpc = false, isNonDefaultVpc = false;

    public AwsInstanceEnvIdentity() {
        String vpcId = getVpcId();
        if (vpcId == null || vpcId.isEmpty()) {
            this.isClassic = true;
        } else {
            this.isNonDefaultVpc = true; // our instances run under a non
                                         // default ("persistence_*") AWS acct
        }
    }

    /*
     * @return the vpc id of the running instance, null if instance is not
     * running within vpc.
     */
    private String getVpcId() {
        InstanceDataRetriever insDataRetriever = new VpcInstanceDataRetriever();
        return insDataRetriever.getVpcId();
    }

    @Override
    public Boolean isClassic() {
        return this.isClassic;
    }

    @Override
    public Boolean isDefaultVpc() {
        return this.isDefaultVpc;
    }

    @Override
    public Boolean isNonDefaultVpc() {
        return this.isNonDefaultVpc;
    }

}
