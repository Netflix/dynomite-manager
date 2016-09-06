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

import com.netflix.dynomitemanager.sidecore.utils.FifoQueue;

/**
 * Unit tests for FifoQueue
 *
 * @author diegopacheco
 *
 */
public class FifoQueueTest {

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidCapacity() {
		FifoQueue<String> fq = new FifoQueue<>(-1);
		fq.adjustAndAdd("A");
		Assert.assertEquals("A", fq.pollFirst());
	}

	@Test
	public void testCreateQueueAndPoll() {

		FifoQueue<String> fq = new FifoQueue<>(5);
		fq.adjustAndAdd("A");
		fq.adjustAndAdd("B");
		fq.adjustAndAdd("C");
		fq.adjustAndAdd("D");
		fq.adjustAndAdd("E");
		fq.adjustAndAdd("F");

		Assert.assertEquals("B", fq.pollFirst());
		Assert.assertEquals("C", fq.pollFirst());
		Assert.assertEquals("D", fq.pollFirst());
		Assert.assertEquals("E", fq.pollFirst());
		Assert.assertEquals("F", fq.pollFirst());

	}

}
