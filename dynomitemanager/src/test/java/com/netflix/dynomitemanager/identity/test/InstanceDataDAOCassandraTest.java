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
public class InstanceDataDAOCassandraTest  {
	
	@Test(expected=RuntimeException.class)
	public void testWorngCinfiguration() throws Exception{
		
		HostSupplier hs = new HostSupplier() {
			@Override
			public Supplier<List<Host>> getSupplier(String clusterName) {
				return null;
			}
		};
		
		InstanceDataDAOCassandra dao = new InstanceDataDAOCassandra(new BlankConfiguration(),hs);
		dao.createInstanceEntry(null);
	}
	
}
