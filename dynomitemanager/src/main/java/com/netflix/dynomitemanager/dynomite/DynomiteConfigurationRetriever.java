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
package com.netflix.dynomitemanager.dynomite;

import com.google.inject.Singleton;

import com.netflix.dynomitemanager.conf.ModuleConfigurationRetriever;

/**
 * Get Dynomite configuration via Archaius provided Fast Properties (FP).
 */
@Singleton
public class DynomiteConfigurationRetriever extends ModuleConfigurationRetriever implements DynomiteConfiguration {

    private static final String MODULE_PREFIX = ".dynomite";

    // Ports
    // =====

    private static final String CLIENT_PORT_CONF = DM_PREFIX + MODULE_PREFIX + ".client.port";
    private static final int CLIENT_PORT_DEFAULT = 8102;

    private static final String PEER_PORT_CONF = DM_PREFIX + MODULE_PREFIX + ".peer.port";
    private static final int PEER_PORT_DEFAULT = 8101;

    @Override
    public int getClientPort() {
        return getIntProperty(CLIENT_PORT_CONF, CLIENT_PORT_DEFAULT);
    }

    @Override
    public int getPeerPort() {
        return getIntProperty(PEER_PORT_CONF, PEER_PORT_DEFAULT);
    }

    // Memory usage
    // ============

    private static final String MBUF_SIZE_CONFIG = DM_PREFIX + MODULE_PREFIX + ".mbuf.size";
    private static final int MBUF_SIZE_DEFAULT = 16384;

    private static final String ALLOCATED_MESSAGES_CONFIG = DM_PREFIX + MODULE_PREFIX + ".allocated.messages";
    private static final int ALLOCATED_MESSAGES_DEFAULT = 200000;

    @Override
    public int getMbufSize() {
        return getIntProperty(MBUF_SIZE_CONFIG, MBUF_SIZE_DEFAULT);
    }

    @Override
    public int getAllocatedMessages() {
        return getIntProperty(ALLOCATED_MESSAGES_CONFIG, ALLOCATED_MESSAGES_DEFAULT);
    }

    // REST API
    // ========

    private String API_BASE_URL_CONFIG = getPropertyKey(MODULE_PREFIX, "api.url");
    private final String API_BASE_URL_DEFAULT = "http://localhost:22222";

    private static final String API_SET_STATE_NORMAL = "/state/normal";
    private static final String API_SET_STATE_RESUMING = "/state/resuming";
    private static final String API_SET_STATE_WRITES_ONLY = "/state/writes_only";

    private static final String API_SET_CONSISTENCY_READ = "/set_consistency/read/";
    private static final String API_SET_CONSISTENCY_WRITE = "/set_consistency/write/";

    @Override
    public String getApiUrl() {
        return getStringProperty(API_BASE_URL_CONFIG, API_BASE_URL_DEFAULT);
    }

    @Override
    public String getApiSetStateNormal() {
        return getApiUrl() + API_SET_STATE_NORMAL;
    }

    @Override
    public String getApiSetStateResuming() {
        return getApiUrl() + API_SET_STATE_RESUMING;
    }

    @Override
    public String getApiSetStateWritesOnly() {
        return getApiUrl() + API_SET_STATE_WRITES_ONLY;
    }

    @Override
    public String getApiSetReadConsistency() {
        return getApiUrl() + API_SET_CONSISTENCY_READ + getReadConsistency();
    }

    @Override
    public String getApiSetWriteConsistency() {
        return getApiUrl() + API_SET_CONSISTENCY_WRITE + getWriteConsistency();
    }

    // Consistency Level
    // =================

    private String READ_CONSISTENCY_LEVEL_CONF = DM_PREFIX + MODULE_PREFIX + ".consistency_level.read";
    private static final String READ_CONSISTENCY_LEVEL_DEFAULT = "DC_ONE";

    private String WRITE_CONSISTENCY_LEVEL_CONF = DM_PREFIX + MODULE_PREFIX + ".consistency_level.write";
    private static final String WRITE_CONSISTENCY_LEVEL_DEFAULT = "DC_ONE";

    @Override
    public String getReadConsistency() {
        return getStringProperty(READ_CONSISTENCY_LEVEL_CONF, READ_CONSISTENCY_LEVEL_DEFAULT);
    }

    @Override
    public String getWriteConsistency() {
        return getStringProperty(WRITE_CONSISTENCY_LEVEL_CONF, WRITE_CONSISTENCY_LEVEL_DEFAULT);
    }

    // Scripts
    // =======

    private String START_SCRIPT_CONF = DM_PREFIX + MODULE_PREFIX + ".start.script";
    private static final String START_SCRIPT_DEFAULT = "/apps/dynomite/bin/launch_dynomite.sh";

    private String STOP_SCRIPT_CONF = DM_PREFIX + MODULE_PREFIX + ".stop.script";
    private static final String STOP_SCRIPT_DEFAULT = "/apps/dynomite/bin/kill_dynomite.sh";

    @Override
    public String getStartScript() {
        return getStringProperty(START_SCRIPT_CONF, START_SCRIPT_DEFAULT);
    }

    @Override
    public String getStopScript() {
        return getStringProperty(STOP_SCRIPT_CONF, STOP_SCRIPT_DEFAULT);
    }

    // Misc settings
    // =============

    private String SEED_PROVIDER_CONF = DM_PREFIX + MODULE_PREFIX + ".seed.provider";
    private static final String SEED_PROVIDER_DEFAULT = "florida_provider";

    @Override
    public String getSeedProvider() {
        return getStringProperty(SEED_PROVIDER_CONF, SEED_PROVIDER_DEFAULT);
    }
}