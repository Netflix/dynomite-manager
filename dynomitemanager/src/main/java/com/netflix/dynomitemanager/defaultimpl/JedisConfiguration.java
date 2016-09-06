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
package com.netflix.dynomitemanager.defaultimpl;

public class JedisConfiguration {
		public static final int DYNO_MEMCACHED = 0;
		public static final int DYNO_REDIS = 1;
		public static final String DYNO_REDIS_CONF_PATH = "/apps/nfredis/conf/redis.conf";
		public static final int DYNO_PORT = 8102;
		public static final String REDIS_ADDRESS = "127.0.0.1";
		public static final int REDIS_PORT = 22122;
}
