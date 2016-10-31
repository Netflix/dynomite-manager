package com.netflix.dynomitemanager.dynomite;

import com.google.inject.ImplementedBy;

/**
 * Get the Dynomite configuration properties provided by Archaius.
 */
@ImplementedBy(DynomiteConfigurationRetriever.class)
public interface DynomiteConfiguration {

    // Ports
    // =====

    int getClientPort();
    int getPeerPort();

    // Memory usage
    // ============

    int getMbufSize();
    int getAllocatedMessages();

    // REST API
    // ========

    /**
     * Get the base URL for the Dynomite REST API.
     * @return the base URL for the Dynomite REST API
     */
    String getApiUrl();

    String getApiSetStateNormal();
    String getApiSetStateResuming();
    String getApiSetStateWritesOnly();

    String getApiSetReadConsistency();
    String getApiSetWriteConsistency();

    // Consistency level
    // =================

    String getReadConsistency();
    String getWriteConsistency();

    // Scripts
    // =======

    String getStartScript();
    String getStopScript();

    // Misc settings
    // =============

    String getSeedProvider();

}
