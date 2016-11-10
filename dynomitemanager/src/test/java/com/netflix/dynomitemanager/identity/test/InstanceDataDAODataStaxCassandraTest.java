package com.netflix.dynomitemanager.identity.test;

import org.junit.Test;

import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.InstanceDataDAODataStaxCassandra;
import com.netflix.dynomitemanager.monitoring.test.BlankConfiguration;
import com.netflix.dynomitemanager.supplier.CassandraLocalHostsSupplier;

/**
 * Unit Tests for InstanceDataDAODataStaxCassandra.
 * 
 * @author diegopacheco
 * @since 11/09/2016
 *
 */
public class InstanceDataDAODataStaxCassandraTest {
	
	@Test
	public void testContructor(){
		IConfiguration ic = new BlankConfiguration();
		new InstanceDataDAODataStaxCassandra(ic,new CassandraLocalHostsSupplier(ic));
	}
	
}
