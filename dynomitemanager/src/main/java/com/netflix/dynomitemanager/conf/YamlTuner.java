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
package com.netflix.dynomitemanager.conf;

import java.io.IOException;

/**
 * Write a yaml file to disk.
 */
public interface YamlTuner {

    /**
     * Generate and write a yaml file to disk.
     *
     * @param yamlLocation full path to a yaml file
     * @throws IOException exception thrown if unable to write yaml file
     */
    void writeAllProperties(String yamlLocation) throws IOException;

}
