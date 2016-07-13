package com.netflix.dynomitemanager.sidecore.utils.test;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.dynomitemanager.sidecore.utils.TokenManager;

/**
 * Unit tests for TokenManager
 * 
 * @author diegopacheco
 *
 */
public class TokenManagerTest {
	
	@Test
	public void createTokenTest(){
		TokenManager tm = new TokenManager();
		String token = tm.createToken(0, 1, "us-west-2");
		Assert.assertNotNull(token);
		Assert.assertTrue( !"".equals(token)  );
		Assert.assertEquals("1383429731", token);
	}
	
	@Test
	public void createToken2Test(){
		TokenManager tm = new TokenManager();
		String token = tm.createToken(1, 2, "us-west-2");
		Assert.assertNotNull(token);
		Assert.assertTrue( !"".equals(token)  );
		Assert.assertEquals("3530913378", token);
	}
	
	@Test
	public void createTokenRackAndSizeTest(){
		TokenManager tm = new TokenManager();
		String token = tm.createToken(1, 2, 2, "us-west-2");
		Assert.assertNotNull(token);
		Assert.assertTrue( !"".equals(token)  );
		Assert.assertEquals("2457171554", token);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createTokenWorngCountTest(){
		TokenManager tm = new TokenManager();
		tm.createToken(0, -1, "us-west-2");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createTokenWorngSlotTest(){
		TokenManager tm = new TokenManager();
		tm.createToken(-1, 0, "us-west-2");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createTokenWorngRackCountTest(){
		TokenManager tm = new TokenManager();
		tm.createToken(1, -1 ,2, "us-west-2");
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void createTokenWorngSizeTest(){
		TokenManager tm = new TokenManager();
		tm.createToken(1, 1 , -1, "us-west-2");
	}
	
	@Test
	public void createRegionOffSet(){
		TokenManager tm = new TokenManager();
		tm.createToken(0, 2, "us-west-2");
		int offSet = tm.regionOffset("us-west-2");
		Assert.assertTrue( offSet >= 1);
		Assert.assertEquals(1383429731, offSet);
	}
	
	
}
