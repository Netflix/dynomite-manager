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
package com.netflix.dynomitemanager.supplier.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.astyanax.connectionpool.Host;
import com.netflix.dynomitemanager.defaultimpl.test.BlankConfiguration;
import com.netflix.dynomitemanager.supplier.CassandraLocalHostsSupplier;

import static org.hamcrest.CoreMatchers.is;

/**
 * Unit Tests for CassandraHostsSupplier
 *
 * @author diegopacheco
 * @author akbarahmed
 */
public class CassandraLocalHostsSupplierTest {

	@Test
	public void testSupplier() {
		CassandraLocalHostsSupplier lhs = new CassandraLocalHostsSupplier(new BlankConfiguration());
		List<Host> hosts = lhs.getSupplier("DmClusterTest").get();
		Assert.assertNotNull(hosts);
		Assert.assertTrue(hosts.get(0) != null);
        Assert.assertThat(hosts.get(0).getPort(), is(9160));
		Assert.assertTrue("localdc".equals(hosts.get(0).getRack()));
		Assert.assertTrue("127.0.0.1".equals(hosts.get(0).getIpAddress()));
	}

}

