/**
 * Copyright 2016 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.dynomitemanager.sidecore.IConfiguration;

public class LocalHostsSupplier implements HostSupplier {

		private IConfiguration config;

		@Inject public LocalHostsSupplier(IConfiguration config) {
				this.config = config;
		}

		@Override public Supplier<List<Host>> getSupplier(String clusterName) {
				final List<Host> hosts = new ArrayList<Host>();

				String bootCluster = config.getBootClusterName();

				if (bootCluster == null)
						bootCluster = "";

				if (bootCluster.equals(clusterName)) {

						String seeds = System.getenv("DM_CASSANDRA_CLUSTER_SEEDS");

						if (seeds == null || "".equals(seeds))
								throw new RuntimeException(
										"Cassandra Host Names can not be blank. At least one host is needed. Please use env var: DM_CASSANDRA_CLUSTER_SEEDS.");

						List<String> cassHostnames = new ArrayList<String>(
								Arrays.asList(StringUtils.split(seeds, ",")));

						if (cassHostnames.size() == 0)
								throw new RuntimeException(
										"Cassandra Host Names can not be blank. At least one host is needed. Please use env var: DM_CASSANDRA_CLUSTER_SEEDS.");

						for (String cassHost : cassHostnames) {
								hosts.add(new Host(cassHost, 9160));
						}

				} else {
						hosts.add(new Host("127.0.0.1", 9160).setRack("localdc"));
				}

				return new Supplier<List<Host>>() {
						@Override public List<Host> get() {
								return hosts;
						}
				};

		}
}
