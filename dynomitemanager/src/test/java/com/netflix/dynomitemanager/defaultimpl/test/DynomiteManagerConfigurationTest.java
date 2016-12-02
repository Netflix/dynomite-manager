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
    public void testGetDynomiteAutoEjectHosts() throws Exception {
        Assert.assertThat("Auto-eject hosts = default", conf.getDynomiteAutoEjectHosts(), is(true));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "false";
            }
        };
        Assert.assertThat("Auto-eject hosts = env var", conf.getDynomiteAutoEjectHosts(), is(false));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Auto-eject hosts = default", conf.getDynomiteAutoEjectHosts(), is(true));
    }

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
    public void testGetDynomiteGossipInterval() throws Exception {
        Assert.assertThat(conf.getDynomiteGossipInterval(), is(10000));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "999";
            }
        };
        Assert.assertThat(conf.getDynomiteGossipInterval(), is(999));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat(conf.getDynomiteGossipInterval(), is(10000));
    }

    @Test
    public void testGetDynomiteHashAlgorithm() throws Exception {
        Assert.assertThat(conf.getDynomiteHashAlgorithm(), is("murmur"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "murmur3";
            }
        };
        Assert.assertThat(conf.getDynomiteHashAlgorithm(), is("murmur3"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat(conf.getDynomiteHashAlgorithm(), is("murmur"));
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
    public void testGetDynomiteIntraClusterSecurity() throws Exception {
        Assert.assertThat("Intra-cluster security = default", conf.getDynomiteIntraClusterSecurity(), is("datacenter"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "none";
            }
        };
        Assert.assertThat("Intra-cluster security = env var", conf.getDynomiteIntraClusterSecurity(), is("none"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Intra-cluster security = default", conf.getDynomiteIntraClusterSecurity(), is("datacenter"));
    }

    @Test
    public void testGetDynomiteMaxAllocatedMessages() throws Exception {
        Assert.assertThat("max allocated messages = default", conf.getDynomiteMaxAllocatedMessages(), is(200000));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "400000";
            }
        };
        Assert.assertThat("max allocated messages = env var", conf.getDynomiteMaxAllocatedMessages(), is(400000));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat("max allocated messages = default", conf.getDynomiteMaxAllocatedMessages(), is(200000));
    }


    @Test
    public void testGetDynomiteMBufSize() throws Exception {
        Assert.assertThat("mbuf size = default", conf.getDynomiteMBufSize(), is(16384));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "32768";
            }
        };
        Assert.assertThat("mbuf size = env var", conf.getDynomiteMBufSize(), is(32768));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat("mbuf size = default", conf.getDynomiteMBufSize(), is(16384));
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
    public void testGetDynomiteProcessName() throws Exception {
        Assert.assertThat("Dynomite process name = default", conf.getDynomiteProcessName(), is("dynomite"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "renamed";
            }
        };
        Assert.assertThat("Dynomite process name = env var", conf.getDynomiteProcessName(), is("renamed"));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Dynomite process name = default", conf.getDynomiteProcessName(), is("dynomite"));
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
    public void testGetDynomiteStoragePreconnect() throws Exception {
        Assert.assertThat("Dynomite storage preconnect = default", conf.getDynomiteStoragePreconnect(), is(true));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "false";
            }
        };
        Assert.assertThat("Dynomite storage preconnect = env var", conf.getDynomiteStoragePreconnect(), is(false));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Dynomite storage preconnect = default", conf.getDynomiteStoragePreconnect(), is(true));
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

    @Test
    public void testIsDynomiteMultiDC() throws Exception {
        Assert.assertThat("Dynomite multi-DC = default", conf.isDynomiteMultiDC(), is(true));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "false";
            }
        };
        Assert.assertThat("Dynomite multi-DC = env var", conf.isDynomiteMultiDC(), is(false));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Dynomite multi-DC = default", conf.isDynomiteMultiDC(), is(true));
    }

    // Storage engine (aka backend)
    // ============================

    @Test
    public void testGetStorageMaxMemoryPercent() throws Exception {
        Assert.assertThat("storage max memory percent = default", conf.getStorageMaxMemoryPercent(), is(85));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "70";
            }
        };
        Assert.assertThat("storage max memory percent = env var", conf.getStorageMaxMemoryPercent(), is(70));
        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat("storage max memory percent = default", conf.getStorageMaxMemoryPercent(), is(85));
    }

    // Cassandra
    // =========

    @Test
    public void testGetCassandraClusterName() throws Exception {
        Assert.assertThat("Cassandra cluster name = default", conf.getCassandraClusterName(), is("cass_dyno"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "dynomite_token_cluster";
            }
        };
        Assert.assertThat("Cassandra cluster name = env var", conf.getCassandraClusterName(), is("dynomite_token_cluster"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Cassandra cluster name = default", conf.getCassandraClusterName(), is("cass_dyno"));
    }

    @Test
    public void testGetCassandraKeyspaceName() throws Exception {
        Assert.assertThat("Cassandra keyspace name = default", conf.getCassandraKeyspaceName(), is("dyno_bootstrap"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "tokens";
            }
        };
        Assert.assertThat("Cassandra keyspace name = env var", conf.getCassandraKeyspaceName(), is("tokens"));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return null;
            }
        };
        Assert.assertThat("Cassandra keyspace name = default", conf.getCassandraKeyspaceName(), is("dyno_bootstrap"));
    }

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

    @Test
    public void testGetCassandraThriftPort() throws Exception {
        Assert.assertThat("Cassandra thrift port = default", conf.getCassandraThriftPort(), is(9160));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "9999";
            }
        };
        Assert.assertThat("Cassandra thrift port = env var", conf.getCassandraThriftPort(), is(9999));

        new MockUp<System>() {
            @Mock
            String getenv(String name) {
                return "not-a-number";
            }
        };
        Assert.assertThat("Cassandra thrift port = default", conf.getCassandraThriftPort(), is(9160));
    }

}
