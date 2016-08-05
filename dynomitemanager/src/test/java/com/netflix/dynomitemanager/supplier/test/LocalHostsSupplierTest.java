package com.netflix.dynomitemanager.supplier.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.astyanax.connectionpool.Host;
import com.netflix.dynomitemanager.supplier.LocalHostsSupplier;

/**
 * Unit Tests for LocalHostsSupplier
 * 
 * @author diegopacheco
 *
 */
public class LocalHostsSupplierTest {

	@Test
	public void testSupplier(){
		LocalHostsSupplier lhs = new LocalHostsSupplier();
		List<Host> hosts = lhs.getSupplier("DmClusterTest").get();
		Assert.assertNotNull(hosts);
		Assert.assertTrue(hosts.get(0) != null);
		Assert.assertTrue(hosts.get(0).getPort() == 9160);
		Assert.assertTrue("localdc".equals(hosts.get(0).getRack()));
		Assert.assertTrue("127.0.0.1".equals(hosts.get(0).getIpAddress()));
	}
	
}

