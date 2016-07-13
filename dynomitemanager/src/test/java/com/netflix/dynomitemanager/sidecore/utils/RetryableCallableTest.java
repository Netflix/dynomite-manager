package com.netflix.dynomitemanager.sidecore.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Tests for RetryableCallable
 * 
 * @author diegopacheco
 *
 */
public class RetryableCallableTest {
	
	@Test
	public void testCallbackOK() throws Exception{
		
		RetryableCallable<String> callback = new RetryableCallable<String>(1,1000L) {
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
	public void testCallbackRetry1() throws Exception{
		
		RetryableCallable<String> callback = new RetryableCallable<String>(2,1000L) {
			boolean first = true;
			@Override
			public String retriableCall() throws Exception {
				if (first){
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
	
	@Test(expected=RuntimeException.class)
	public void testCallbackRetry1WorngRetrys() throws Exception{
		
		RetryableCallable<String> callback = new RetryableCallable<String>(-1,1000L) {
			boolean first = true;
			@Override
			public String retriableCall() throws Exception {
				if (first){
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
	
	@Test(expected=RuntimeException.class)
	public void testCallbackRetry1WorngTime() throws Exception{
		
		RetryableCallable<String> callback = new RetryableCallable<String>(1,-1000L) {
			boolean first = true;
			@Override
			public String retriableCall() throws Exception {
				if (first){
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
