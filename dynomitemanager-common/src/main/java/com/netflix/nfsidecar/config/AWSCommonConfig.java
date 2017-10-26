/**
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.nfsidecar.config;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "dbsidecar.aws")
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
