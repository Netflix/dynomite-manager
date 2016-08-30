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
package com.netflix.dynomitemanager.sidecore.utils.test;

import org.junit.Assert;
import org.junit.Test;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.sidecore.utils.FloridaHealthCheckHandler;

/**
 * FloridaHealthCheckHandler unit tests
 *
 * @author diegopacheco
 *
 */
public class FloridaHealthCheckHandlerTest {

	@Test
	public void testHandlerBootstrapping() {
		InstanceState state = new InstanceState() {
			public boolean isBootstrapping() {
				return true;
			}

			;
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(503, fhc.getStatus());
	}

	@Test
	public void testHandlerNotHealthy() {
		InstanceState state = new InstanceState() {
			public boolean isHealthy() {
				return false;
			}

			;
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(503, fhc.getStatus());
	}

	@Test
	public void testHandlerOK() {
		InstanceState state = new InstanceState() {
			public boolean isHealthy() {
				return true;
			}

			;
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(200, fhc.getStatus());
	}

}
