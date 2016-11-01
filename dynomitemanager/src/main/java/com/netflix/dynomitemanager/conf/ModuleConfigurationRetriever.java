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

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

/**
 * Base class that provides common functionality required by all module-specific configuration. Every module's
 * configuration properties retriever must extend from this class.
 */
public class ModuleConfigurationRetriever implements ModuleConfiguration {

    // Default prefix for all Dynomite Manager properties
    public static String DM_PREFIX = "dm";

    /**
     * Get a module property's key that is created as application prefix + module prefix + key. Application prefix is
     * the prefix applied to all property keys.
     *
     * @param modulePrefix a module prefix, such as "dynomite", "aws", "cassandra" or similar
     * @param key          the property's key component
     * @return a module-level property key that is used to set a fast property in a .properties file or in a property
     * provider
     */
    protected String getPropertyKey(String modulePrefix, String key) {
        return DM_PREFIX + "." + modulePrefix + "." + key;
    }

    @Override
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        DynamicBooleanProperty property = DynamicPropertyFactory.getInstance().getBooleanProperty(key, defaultValue);
        return property.get();
    }

    @Override
    public int getIntProperty(String key, int defaultValue) {
        DynamicIntProperty property = DynamicPropertyFactory.getInstance().getIntProperty(key, defaultValue);
        return property.get();
    }

    @Override
    public String getStringProperty(String key, String defaultValue) {
        DynamicStringProperty property = DynamicPropertyFactory.getInstance().getStringProperty(key, defaultValue);
        return property.get();
    }

}
