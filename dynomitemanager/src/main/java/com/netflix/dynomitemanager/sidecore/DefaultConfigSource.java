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
package com.netflix.dynomitemanager.sidecore;

import javax.inject.Inject;

/**
 * DefaultConfigSource specifies the list of configuration sources from which to obtain Dynomite Manager's
 * configuration by calling the {@link com.netflix.dynomitemanager.sidecore.CompositeConfigSource} constructor.
 *
 * Load Dynomite Manager's configuration from the following sources:
 *
 * <ul>
 * <li>{@link com.netflix.dynomitemanager.sidecore.SimpleDBConfigSource}
 * <li>{@link com.netflix.dynomitemanager.sidecore.PropertiesConfigSource}
 * <li>{@link com.netflix.dynomitemanager.sidecore.SystemPropertiesConfigSource}
 * </ul>
 */
public class DefaultConfigSource extends CompositeConfigSource {

	@Inject
	public DefaultConfigSource(final PropertiesConfigSource simpleDBConfigSource,
			final PropertiesConfigSource propertiesConfigSource,
			final SystemPropertiesConfigSource systemPropertiesConfigSource) {
		super(simpleDBConfigSource, propertiesConfigSource, systemPropertiesConfigSource);
	}

}
