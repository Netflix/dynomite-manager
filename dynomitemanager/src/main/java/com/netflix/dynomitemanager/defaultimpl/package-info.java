/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Initialize Dynomite Manager and bind dependencies, setup Dynomite Manager configuration, write dynomite.yaml and
 * redis.conf, start/stop Dynomite, start/stop the storage engine (Redis or Memcached), and configure the Jedis client
 * for Redis.
 */
package com.netflix.dynomitemanager.defaultimpl;