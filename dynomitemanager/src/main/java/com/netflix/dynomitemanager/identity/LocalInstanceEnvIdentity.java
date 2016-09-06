/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.identity;

public class LocalInstanceEnvIdentity implements InstanceEnvIdentity {

		@Override public Boolean isClassic() {
				// TODO exemplar local implementation
				return true;
		}

		@Override public Boolean isDefaultVpc() {
				// TODO exemplar local implementation
				return false;
		}

		@Override public Boolean isNonDefaultVpc() {
				// TODO exemplar local implementation
				return false;
		}

}
