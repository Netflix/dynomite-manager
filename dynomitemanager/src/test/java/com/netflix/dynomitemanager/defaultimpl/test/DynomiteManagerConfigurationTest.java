/**
 * Copyright 2016 Netflix, Inc.
 *
 * Copyright 2016 DynomiteDB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.defaultimpl.test;

import com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;
import com.netflix.dynomitemanager.sidecore.IConfigSource;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;

/**
 * DynomiteManagerConfiguration unit test to check Archaius provided configuration.
 */
@RunWith(JMockit.class)
public class DynomiteManagerConfigurationTest {
    @Mocked ICredential credential;
    @Mocked IConfigSource configSource;
    @Mocked InstanceDataRetriever instanceDataRetriever;
    @Mocked InstanceEnvIdentity instanceEnvIdentity;
    @Mocked IStorageProxy storageProxy;

    IConfiguration conf;

    @BeforeClass
    public static void onlyOnce() {
        Assert.assertNull(System.getenv("DM_CASSANDRA_SEEDS"));
    }

    @Before
    public void runBeforeTests() {
        conf = new DynomitemanagerConfiguration(credential, configSource, instanceDataRetriever, instanceEnvIdentity,
                storageProxy);
    }

    //    @Test
    //    public void testGetDynomiteClientPort() throws Exception {
    //        Assert.assertThat(conf.getDynomiteClientPort(), is(8102));
    //    }

    // Cassandra
    // =========

    @Test
    public void testGetCassandraSeeds() throws Exception {
        Assert.assertThat(conf.getCassandraSeeds(), is("127.0.0.1"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "10.0.0.10";
            }
        };
        Assert.assertThat(conf.getCassandraSeeds(), is("10.0.0.10"));
    }

}
