/**
 * Copyright 2016 Netflix, Inc.
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

import java.io.IOException;

import com.google.inject.ImplementedBy;

/**
 * Manage the Dynomite process including start/stop and health check.
 */
@ImplementedBy(DynomiteProcessManager.class)
public interface IDynomiteProcess {

    /**
     * Start the Dynomite process.
     *
     * @throws IOException exception if Dynomite does not start
     */
    void start() throws IOException;

    /**
     * Stop the Dynomite process.
     *
     * @throws IOException exception if Dynomite does not stop
     */
    void stop() throws IOException;

    /**
     * Perform a health check on Dynomite.
     *
     * @return true if Dynomite is healthy or false if there is a problem with Dynomite
     */
    boolean dynomiteCheck();

}
