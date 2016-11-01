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

/**
 * Dynomite Manager and module configuration. Configuration includes two distinct types of configuration:
 *
 * <ul>
 * <li>Java properties: Properties are inbound configuration for Dynomite Manager, Dynomite, Redis, ARDB, etc. that are
 * provided via Archaius.
 * <li>Configuration files: Configuration files are outbound configuration used to configure processes under the control
 * of Dynomite Manager, such as dynomite.yaml (Dynomite), redis.conf (Redis) and ardb.conf (ARD
 * </ul>
 */
package com.netflix.dynomitemanager.conf;