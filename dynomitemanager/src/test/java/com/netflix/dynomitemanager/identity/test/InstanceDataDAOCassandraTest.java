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
package com.netflix.dynomitemanager.identity.test;

import java.util.List;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.dynomitemanager.identity.InstanceDataDAOCassandra;
import com.netflix.dynomitemanager.monitoring.test.BlankConfiguration;
import com.netflix.dynomitemanager.supplier.HostSupplier;

/**
 * Unit Tests for InstanceDataDAOCassandra
 *
 * @author diegopacheco
 *
 */
public class InstanceDataDAOCassandraTest {

	@Test(expected = RuntimeException.class)
	public void testWorngCinfiguration() throws Exception {

		HostSupplier hs = new HostSupplier() {
			@Override
			public Supplier<List<Host>> getSupplier(String clusterName) {
				return null;
			}
		};

		InstanceDataDAOCassandra dao = new InstanceDataDAOCassandra(new BlankConfiguration(), hs);
		dao.createInstanceEntry(null);
	}

}
