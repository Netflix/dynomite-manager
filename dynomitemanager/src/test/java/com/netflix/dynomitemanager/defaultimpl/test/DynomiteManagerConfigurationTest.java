/**
 * Copyright 2016 Netflix, Inc. and [Dynomite Manager contributors](https://github.com/Netflix/dynomite-manager/blob/dev/CONTRIBUTORS.md)
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

import com.netflix.dynomitemanager.defaultimpl.DynomiteManagerConfiguration;
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
        conf = new DynomiteManagerConfiguration(credential, configSource, instanceDataRetriever, instanceEnvIdentity,
                storageProxy);
    }

    // Dynomite
    // ========

    @Test
    public void testGetDynomiteClientPort() throws Exception {
        Assert.assertThat(conf.getDynomiteClientPort(), is(8102));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "1111";
            }
        };
        Assert.assertThat(conf.getDynomiteClientPort(), is(1111));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat(conf.getDynomiteClientPort(), is(8102));
    }

    @Test
    public void testGetDynomiteClusterName() throws Exception {
        Assert.assertThat("Cluster name = default", conf.getDynomiteClusterName(), is("dynomite_demo1"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "testcluster";
            }
        };
        Assert.assertThat("Cluster name = env var", conf.getDynomiteClusterName(), is("testcluster"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Cluster name = default", conf.getDynomiteClusterName(), is("dynomite_demo1"));
    }

    @Test
    public void testGetDynomiteInstallDir() throws Exception {
        Assert.assertThat("Install dir = default", conf.getDynomiteInstallDir(), is("/apps/dynomite"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "/etc/dynomite";
            }
        };
        Assert.assertThat("Install dir = env var", conf.getDynomiteInstallDir(), is("/etc/dynomite"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Install dir = default", conf.getDynomiteInstallDir(), is("/apps/dynomite"));
    }

    @Test
    public void testGetDynomitePeerPort() throws Exception {
        Assert.assertThat(conf.getDynomitePeerPort(), is(8101));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "2222";
            }
        };
        Assert.assertThat(conf.getDynomitePeerPort(), is(2222));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat(conf.getDynomitePeerPort(), is(8101));
    }

    @Test
    public void testGetDynomiteSeedProvider() throws Exception {
        Assert.assertThat("Seed provider = default", conf.getDynomiteSeedProvider(), is("florida_provider"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "simple_provider";
            }
        };
        Assert.assertThat("Seed provider = env var", conf.getDynomiteSeedProvider(), is("simple_provider"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Seed provider = default", conf.getDynomiteSeedProvider(), is("florida_provider"));
    }

    @Test
    public void testGetDynomiteStartScript() throws Exception {
        Assert.assertThat("Start script = default", conf.getDynomiteStartScript(), is("/apps/dynomite/bin/launch_dynomite.sh"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "/etc/init.d/dynomite-manager";
            }
        };
        Assert.assertThat("Start script = env var", conf.getDynomiteStartScript(), is("/etc/init.d/dynomite-manager"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Start script = default", conf.getDynomiteStartScript(), is("/apps/dynomite/bin/launch_dynomite.sh"));
    }

    @Test
    public void testGetDynomiteStopScript() throws Exception {
        Assert.assertThat("Stop script = default", conf.getDynomiteStopScript(), is("/apps/dynomite/bin/kill_dynomite.sh"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "/etc/init.d/dynomite-manager";
            }
        };
        Assert.assertThat("Stop script = env var", conf.getDynomiteStopScript(), is("/etc/init.d/dynomite-manager"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Stop script = default", conf.getDynomiteStopScript(), is("/apps/dynomite/bin/kill_dynomite.sh"));
    }

    @Test
    public void testGetDynomiteYaml() {
        Assert.assertThat("dynomite.yml = default", conf.getDynomiteYaml(), is("/apps/dynomite/conf/dynomite.yml"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "/etc/dynomite/dynomite.yml";
            }
        };
        Assert.assertThat("dynomite.yml = env var", conf.getDynomiteYaml(), is("/etc/dynomite/dynomite.yml"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "relative/dynomite.yml";
            }
        };
        // This one's a bit odd. getDynomiteYaml() calls getDynomiteInstallDir(). Both return the fake env var above, so
        // the result is a duplicate of the value above. Don't worry, this is working as expected and will give the
        // correct path during runtime.
        Assert.assertThat("dynomite.yml = env var", conf.getDynomiteYaml(), is("relative/dynomite.yml/relative/dynomite.yml"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("dynomite.yml = default", conf.getDynomiteYaml(), is("/apps/dynomite/conf/dynomite.yml"));
    }

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
