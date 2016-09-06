/**
 * Copyright 2016 Netflix, Inc. <p/> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p/>
 * http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.netflix.dynomitemanager.sidecore.utils.test;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.dynomitemanager.sidecore.utils.RetryableCallable;

/**
 * Unit Tests for RetryableCallable
 *
 * @author diegopacheco
 *
 */
public class RetryableCallableTest {

	@Test
	public void testCallbackOK() throws Exception {

		RetryableCallable<String> callback = new RetryableCallable<String>(1, 1000L) {
			@Override
			public String retriableCall() throws Exception {
				return "OK";
			}
		};
		String result = callback.call();

		Assert.assertNotNull(result);
		Assert.assertEquals("OK", result);
	}

	@Test
	public void testCallbackRetry1() throws Exception {

		RetryableCallable<String> callback = new RetryableCallable<String>(2, 1000L) {
			boolean first = true;

			@Override
			public String retriableCall() throws Exception {
				if (first) {
					first = false;
					throw new RuntimeException("We should fail 1 time at leat, but this is ok.");
				}
				return "OK";
			}
		};
		String result = callback.call();

		Assert.assertNotNull(result);
		Assert.assertEquals("OK", result);
	}

	@Test(expected = RuntimeException.class)
	public void testCallbackRetry1WorngRetrys() throws Exception {

		RetryableCallable<String> callback = new RetryableCallable<String>(-1, 1000L) {
			boolean first = true;

			@Override
			public String retriableCall() throws Exception {
				if (first) {
					first = false;
					throw new RuntimeException("We should fail 1 time at leat, but this is ok.");
				}
				return "OK";
			}
		};
		String result = callback.call();

		Assert.assertNotNull(result);
		Assert.assertEquals("OK", result);
	}

	@Test(expected = RuntimeException.class)
	public void testCallbackRetry1WorngTime() throws Exception {

		RetryableCallable<String> callback = new RetryableCallable<String>(1, -1000L) {
			boolean first = true;

			@Override
			public String retriableCall() throws Exception {
				if (first) {
					first = false;
					throw new RuntimeException("We should fail 1 time at leat, but this is ok.");
				}
				return "OK";
			}
		};
		String result = callback.call();

		Assert.assertNotNull(result);
		Assert.assertEquals("OK", result);
	}

}
