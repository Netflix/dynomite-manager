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
	public void testHandlerBootstrapping(){
		InstanceState state = new InstanceState(){
			public boolean isBootstrapping() {
				return true;
			};
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(503, fhc.getStatus() ); 
	}
	
	@Test
	public void testHandlerNotHealthy(){
		InstanceState state = new InstanceState(){
			public boolean isHealthy() {
				return false;
			};
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(503, fhc.getStatus() ); 
	}
	
	@Test
	public void testHandlerOK(){
		InstanceState state = new InstanceState(){
			public boolean isHealthy() {
				return true;
			};
		};
		FloridaHealthCheckHandler fhc = new FloridaHealthCheckHandler(state);
		Assert.assertEquals(200, fhc.getStatus() ); 
	}
	
}
