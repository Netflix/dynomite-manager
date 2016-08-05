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
	
	@Test
	public void testCallbackRetry4() throws Exception {
		BoundedExponentialRetryCallable<String> callback = new BoundedExponentialRetryCallable<String>() {
			int retryCount = 1;
			@Override
			public String retriableCall() throws Exception {
				if (retryCount<=3){
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
