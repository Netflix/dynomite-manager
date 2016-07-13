package com.netflix.dynomitemanager.sidecore.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit Tests for SystemUtils
 * 
 * @author diegopacheco
 *
 */
public class SystemUtilsTest {
	
	@Test
	public void getDatafromURLTest(){
		String data = SystemUtils.getDataFromUrl("http://www.google.com/");
		Assert.assertNotNull(data);
		Assert.assertTrue(!"".equals(data) );
	}
	
	@Test(expected=RuntimeException.class)
	public void getDatafromURLNullTest(){
		SystemUtils.getDataFromUrl(null);
	}
	
	@Test
	public void createDirTest() throws IOException {
		String tmpdir = System.getProperty("java.io.tmpdir");
		SystemUtils.createDirs(tmpdir + "/dm/test");
		Assert.assertTrue(  new File(tmpdir + "/dm/test").exists() );
		
		FileUtils.deleteDirectory(new File(tmpdir + "/dm/test"));
	}
	
	@Test
	public void cleanUpDirTest() throws IOException {
		String tmpdir = System.getProperty("java.io.tmpdir");
		SystemUtils.createDirs(tmpdir + "/dm/test");
		Assert.assertTrue(  new File(tmpdir + "/dm/test").exists() );
		
		List<String> childDirs = Arrays.asList("dm");
		SystemUtils.cleanupDir(tmpdir, childDirs);
		Assert.assertTrue(  !new File(tmpdir + "/dm/test").exists() );
	}
	
	@Test
	public void getMD5Test(){
		byte[] buf = new byte[]{0,0,0,0,0};
		byte[] res = SystemUtils.md5(buf);
		Assert.assertNotNull(res);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void rrongBufMD5Test(){
		byte[] buf = null;
		byte[] res = SystemUtils.md5(buf);
		Assert.assertNotNull(res);
	}	
	
	@Test
	public void fileMD5Test() throws IOException{
		String md5 = SystemUtils.md5( new File(new File(".").getCanonicalPath() + "/src/test/resources/file_md5_test.txt"));
		Assert.assertNotNull(md5);
		Assert.assertEquals("0167cd2278106892573571ef1cfb7d24", md5);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullFileMD5Test() {
		File f = null;
		SystemUtils.md5(f);
	}
	
	@Test
	public void toHexTest() {
		byte[] digest = new byte[]{0,0,0,0,0};
		String hex = SystemUtils.toHex(digest);
		Assert.assertNotNull(hex);
		Assert.assertTrue( !"".equals(hex) );
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void toHexNullTest() {
		byte[] digest = null;
		String hex = SystemUtils.toHex(digest);
		Assert.assertNotNull(hex);
		Assert.assertTrue( !"".equals(hex) );
	}
	
	@Test
	public void toBase64Test() {
		byte[] digest = new byte[]{0,0,0,0,0};
		byte[] md5    = SystemUtils.md5(digest);
		
		String base64 = SystemUtils.toBase64(md5);
		Assert.assertNotNull(base64);
		Assert.assertTrue( !"".equals(base64) );
		Assert.assertEquals("ypxJGsZrLGJQCILpPzcZqA==", base64);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void toBase64NullTest() {
		byte[] md5    = null;
		SystemUtils.toBase64(md5);
	}
	
}
