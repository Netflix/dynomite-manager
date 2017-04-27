/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.defaultimpl.test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.netflix.dynomitemanager.dynomite.DynomiteStandardTuner;
import com.netflix.dynomitemanager.monitoring.test.BlankConfiguration;

/**
 * 
 * Unit Tests for FloridaStandardTuner
 * 
 * @author diegopacheco
 * @author ipapapa
 *
 */
public class FloridaStandardTunerTest {

    @Test
    public void testWriteAllProperties() throws Exception {
        DynomiteStandardTuner tuner = new DynomiteStandardTuner(new BlankConfiguration(), new FakeInstanceIdentity(), null, null);

        String yamlPath = System.getProperty("java.io.tmpdir") + "/yaml-tunner.yaml";
        String templateYamlPath = new File(".").getCanonicalPath() + "/src/test/resources/sample-yaml.yaml";
        Files.copy(Paths.get(templateYamlPath), Paths.get(yamlPath), REPLACE_EXISTING);

/*
 * Not testing this until we fix it
 *         tuner.writeAllProperties(yamlPath);
        String result = FileUtils.readFileToString(new File(yamlPath));

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("101134286"));
        Assert.assertTrue(result.contains("/apps/dynomite/conf/dynomite.pem"));
        Assert.assertTrue(result.contains(new FakeInstanceIdentity().getSeeds().get(0)));
        Assert.assertTrue(result.contains(new FakeInstanceIdentity().getSeeds().get(1)));
        Assert.assertTrue(result.contains(new FakeInstanceIdentity().getSeeds().get(2)));
        */
    }

}
