/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.config;

import com.netflix.dynomitemanager.sidecore.utils.SystemUtils;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;

/**
 * Get AWS EC2 metadata for the current instance when the instance is in an AWS classic network.
 */
public class AwsInstanceDataRetriever implements InstanceDataRetriever {

    public String getDataCenter() {
        String az = getRac();
        String region = "";
        if (az != null && az.length() > 0) {
            region = az.substring(0, az.length()-1);
        }
        return region;
    }

	public String getRac() {
		return SystemUtils
				.getDataFromUrl("http://169.254.169.254/latest/meta-data/placement/availability-zone");
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

	/**
	 * Get the network interface for this instance.
	 */
	@Override
	public String getMac() {
		return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/network/interfaces/macs/")
				.trim();
	}

	@Override
	public String getVpcId() {
		throw new UnsupportedOperationException("Not applicable as running instance is in classic environment");
	}

	@Override
	public String getSecurityGroupName() {
		throw new UnsupportedOperationException("Not applicable as running instance is in classic environment");
	}

}
