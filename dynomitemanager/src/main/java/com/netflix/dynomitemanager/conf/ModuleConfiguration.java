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
package com.netflix.dynomitemanager.conf;

import com.google.inject.ImplementedBy;

/**
 * Generic functionality that is common to all module configuration.
 */
@ImplementedBy(ModuleConfigurationRetriever.class)
public interface ModuleConfiguration {

    /**
     * Get a string value from Archaius.
     *
     * @param key          the property's key
     * @param defaultValue the property's default value
     * @return the property's value as returned by Archaius
     */
    String getStringProperty(String key, String defaultValue);

    /**
     * Get an int value from Archaius.
     *
     * @param key          the property's key
     * @param defaultValue the property's default value
     * @return the property's value as returned by Archaius
     */
    int getIntProperty(String key, int defaultValue);

}
