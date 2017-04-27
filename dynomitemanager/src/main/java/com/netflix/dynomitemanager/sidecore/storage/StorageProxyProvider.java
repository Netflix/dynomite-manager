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
package com.netflix.dynomitemanager.sidecore.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.sidecore.IConfiguration;

@Singleton
public class StorageProxyProvider {

    @Inject
    private IConfiguration config;
    private StorageProxy storageProxy;
    
    
    public StorageProxy getStorageProxy() 
    {
        if (config.getClusterType() == 0) {  //memcached
            if (storageProxy == null) {
                storageProxy = new MemcachedStorageProxy();
            }
        } else if (config.getClusterType() == 1) {
            if (storageProxy == null) {
                storageProxy = new RedisStorageProxy();
            }            
        }
        
        return storageProxy;
    }

}
