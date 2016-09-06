/**
 * Copyright 2016 Netflix, Inc. <p/> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p/>
 * http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.netflix.dynomitemanager.supplier.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Supplier;
import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.AmazonInfo.MetaDataKey;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import com.netflix.dynomitemanager.supplier.EurekaHostsSupplier;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

/**
 * Unit Tests for EurekaHostsSupplier
 *
 * @author diegopacheco
 *
 */
@RunWith(JMockit.class)
public class EurekaHostsSupplierTest {

	@Mocked(methods = { "getApplication" }) private DiscoveryClient dc;

	@Mocked(methods = { "getInstances" }) private Application app;

	@Mocked private InstanceInfo ii;

	@Test
	public void testGetSuppliers() {

		final List<InstanceInfo> instances = new ArrayList<InstanceInfo>();
		instances.add(ii);

		final AmazonInfo ai = new AmazonInfo();
		Map<String, String> metadata = new HashMap<>();
		metadata.put(MetaDataKey.availabilityZone.getName(), "us-west-2a");
		ai.setMetadata(metadata);

		new Expectations() {{
			dc.getApplication("DM_TEST");
			result = app;
			app.getInstances();
			result = instances;

			ii.getStatus();
			result = InstanceInfo.InstanceStatus.UP;
			ii.getStatus();
			result = InstanceInfo.InstanceStatus.UP;
			ii.getHostName();
			result = "ec2-00-000-000-00.us-west-2.compute.amazonaws.com";
			ii.getHostName();
			result = "ec2-00-000-000-00.us-west-2.compute.amazonaws.com";
			ii.getPort();
			result = 8080;
			ii.getIPAddr();
			result = "127.0.0.1";
			ii.getId();
			result = "id-1";
			ii.getDataCenterInfo();
			result = ai;
			ii.getDataCenterInfo();
			result = ai;
		}};

		EurekaHostsSupplier ehs = new EurekaHostsSupplier(dc);
		Supplier<List<Host>> sup = ehs.getSupplier("DM_TEST");
		List<Host> result = sup.get();

		Assert.assertTrue(result != null);
		Assert.assertTrue(result.get(0) != null);
		Assert.assertTrue("ec2-00-000-000-00.us-west-2.compute.amazonaws.com"
				.equals(result.get(0).getHostName()));
		Assert.assertTrue("ec2-00-000-000-00.us-west-2.compute.amazonaws.com"
				.equals(result.get(0).getIpAddress()));
		Assert.assertTrue(8080 == result.get(0).getPort());

	}

	@Test(expected = RuntimeException.class)
	public void testGetSuppliersWithNull() {
		EurekaHostsSupplier ehs = new EurekaHostsSupplier(null);
		Supplier<List<Host>> sup = ehs.getSupplier(null);
		sup.get();
	}

}
