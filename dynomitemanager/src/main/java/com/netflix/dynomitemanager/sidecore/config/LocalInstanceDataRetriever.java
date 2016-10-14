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

/**
 * Looks at local (system) properties for metadata about the running 'instance'.
 * Typically, this is used for locally-deployed testing.
 *
 */
public class LocalInstanceDataRetriever implements InstanceDataRetriever {
	private static final String PREFIX = "dynomitemanager.localInstance.";

    public String getDataCenter() {
        return "us-east-1";
    }

	public String getRac() {
		return "us-east-1c";
	}

	public String getPublicHostname() {
		return "localhost";
	}

	public String getPublicIP() {
		return "127.0.0.1";
	}

	public String getInstanceId() {
		return "i-dynomite";
	}

	public String getInstanceType() {
		return "r3.2xlarge";
	}

	public String getMac() {
		return System.getProperty(PREFIX + "instanceMac", "");
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
