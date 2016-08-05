package com.netflix.dynomitemanager.defaultimpl.test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.dynomitemanager.defaultimpl.FloridaStandardTuner;
import com.netflix.dynomitemanager.monitoring.test.BlankConfiguration;

/**
 * 
 * Unit Tests for FloridaStandardTuner
 * 
 * @author diegopacheco
 *
 */
public class FloridaStandardTunerTest {
	
	@Test
	public void testWriteAllProperties() throws Exception{
		FloridaStandardTuner tuner = new FloridaStandardTuner(new BlankConfiguration(), new FakeInstanceIdentity());
		
		String yamlPath         = System.getProperty("java.io.tmpdir") + "/yaml-tunner.yaml";
		String templateYamlPath = new File(".").getCanonicalPath() + "/src/test/resources/sample-yaml.yaml";
		Files.copy(Paths.get(templateYamlPath), Paths.get(yamlPath), REPLACE_EXISTING);
		
		tuner.writeAllProperties( yamlPath, "localhost", "florida_provider");
		String result = FileUtils.readFileToString(new File(yamlPath));
		
		Assert.assertNotNull(result);
		Assert.assertTrue( result.contains("101134286") );
		Assert.assertTrue( result.contains("/apps/dynomite/conf/dynomite.pem") );
		Assert.assertTrue( result.contains(new FakeInstanceIdentity().getSeeds().get(0)) );
		Assert.assertTrue( result.contains(new FakeInstanceIdentity().getSeeds().get(1)) );
		Assert.assertTrue( result.contains(new FakeInstanceIdentity().getSeeds().get(2)) );
	}
	
}
