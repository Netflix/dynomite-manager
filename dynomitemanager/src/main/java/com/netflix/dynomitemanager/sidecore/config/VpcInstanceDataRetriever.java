/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.config;

import com.netflix.dynomitemanager.sidecore.utils.SystemUtils;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.config.VpcInstanceDataRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls AWS ec2 metadata to get info on the location of the running instance in VPC.
 * Public Hostname will return local-hostname
 * Public IP will return local-ipv4
 */
public class VpcInstanceDataRetriever implements InstanceDataRetriever {
		private static final Logger logger = LoggerFactory.getLogger(VpcInstanceDataRetriever.class);

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

		@Override public String getSecurityGroupName() {
				String mac = getMac();
				return SystemUtils.getDataFromUrl(
						"http://169.254.169.254/latest/meta-data/network/interfaces/macs/" + mac + "/security-groups")
						.trim();
		}

		@Override
	/*
	 * @return id of the network interface for running instance
	 */ public String getMac() {
				return SystemUtils.getDataFromUrl("http://169.254.169.254/latest/meta-data/network/interfaces/macs/")
						.trim();
		}

		@Override
	/*
	 * @return the id of the vpc account for running instance, null if does not exist.
	 */ public String getVpcId() {
				String nacId = getMac();
				if (nacId == null || nacId.isEmpty())
						return null;

				String vpcId = null;
				try {
						vpcId = SystemUtils.getDataFromUrl(
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
