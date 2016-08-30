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

import com.netflix.dynomitemanager.sidecore.utils.BoundedExponentialRetryCallable;

/**
 * Unit Tests for: BoundedExponentialRetryCallable
 *
 * @author diegopacheco
 *
 */
public class BoundedExponentialRetryCallableTest {

	@Test
	public void testCallbackOK() throws Exception {

		BoundedExponentialRetryCallable<String> boundedExponentialRetryCallable = new BoundedExponentialRetryCallable<String>() {
			@Override
			public String retriableCall() throws Exception {
				return "OK";
			}
		};

		String result = boundedExponentialRetryCallable.call();
		Assert.assertNotNull(result);
		Assert.assertEquals("OK", result);

	}

	@Test
	public void testCallbackRetry1() throws Exception {

		BoundedExponentialRetryCallable<String> callback = new BoundedExponentialRetryCallable<String>() {
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

	@Test
	public void testCallbackRetry4() throws Exception {
		BoundedExponentialRetryCallable<String> callback = new BoundedExponentialRetryCallable<String>() {
			int retryCount = 1;

			@Override
			public String retriableCall() throws Exception {
				if (retryCount <= 3) {
					retryCount++;
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
