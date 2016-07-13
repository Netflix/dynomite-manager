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
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidCapacity(){
		FifoQueue<String> fq = new FifoQueue<>(-1);
		fq.adjustAndAdd("A");
		Assert.assertEquals("A", fq.pollFirst());
	}
	
	@Test
	public void testCreateQueueAndPoll(){
		
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
