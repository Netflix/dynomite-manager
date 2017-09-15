package com.netflix.nfsidecar.config;

import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

public interface AWSCommonConfig {

    // Dual Account
    /*
     * @return the Amazon Resource Name (ARN) for EC2 classic.
     */
    @DefaultValue("null")
    @PropertyName(name = "ec2.roleassumption.arn")
    public String getClassicAWSRoleAssumptionArn();

    /*
     * @return the Amazon Resource Name (ARN) for VPC.
     */
    @DefaultValue("null")
    @PropertyName(name = "vpc.roleassumption.arn")
    public String getVpcAWSRoleAssumptionArn();

    @DefaultValue("false")
    @PropertyName(name = "roleassumption.dualaccount")
    public boolean isDualAccount();

    // Backup and Restore

    @DefaultValue("us-east-1.dynomite-backup-test")
    @PropertyName(name = "dyno.backup.bucket.name") // TODO: For a common
                                                    // default value we probably
                                                    // have to result to defined
                                                    // FP
    public String getBucketName();

    @DefaultValue("backup")
    @PropertyName(name = "dyno.backup.s3.base_dir") // TODO: For a common
                                                    // default value we probably
                                                    // have to result to defined
                                                    // FP
    public String getBackupLocation();

}
