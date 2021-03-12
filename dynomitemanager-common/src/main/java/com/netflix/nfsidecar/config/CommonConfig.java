/**
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.nfsidecar.config;

import java.util.List;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "dbsidecar.common")
public interface CommonConfig {
    
    /**
     * @return Get the Region name
     */
    @DefaultValue("")
    @PropertyName(name = "region")
    public String getRegion();
    
    @DefaultValue("")
    @PropertyName(name = "rack")
    public String getRack();
    
    @PropertyName(name = "zones.available")
    public List<String> getRacks();
    

    /**
     * Get the security group associated with nodes in this cluster
     */
    @PropertyName(name = "acl.groupname")
    public String getACLGroupName();
    
    /*****************************************************************/

    /**
     * Get the peer-to-peer port used by Dynomite to communicate with other
     * Dynomite nodes.
     *
     * @return the peer-to-peer port used for intra-cluster communication
     */
    @DefaultValue("8101")
    @PropertyName(name = "dyno.peer.port")
    public int getDynomitePeerPort();

    @DefaultValue("8102")
    @PropertyName(name = "dyno.port")
    public int getDynomitePort();

    @DefaultValue("22222")
    @PropertyName(name = "dyno.stats.port")
    public int getDynomiteStatsPort();

    @DefaultValue("true")
    @PropertyName(name = "dyno.stats.localhost.only")
    public boolean isDynomiteStatsLocalHostOnly();

    // Default value of -1 means it is not having a secure port
    @DefaultValue("-1")
    @PropertyName(name = "dyno.secure.port")
    public int getDynomiteSecurePort();

    // Default value of -1 means it is not having a secure port
    @DefaultValue("-1")
    @PropertyName(name = "dyno.secure.storage.port")
    public int getDynomiteSecureStoragePort();

    @DefaultValue("false")
    @PropertyName(name = "dyno.backup.snapshot.enabled")
    public boolean isBackupEnabled();

    @DefaultValue("false")
    @PropertyName(name = "dyno.backup.restore.enabled")
    public boolean isRestoreEnabled();

    @DefaultValue("day")
    @PropertyName(name = "dyno.backup.schedule")
    public String getBackupSchedule();

    @DefaultValue("12")
    @PropertyName(name = "dyno.backup.hour")
    public int getBackupHour();

    @DefaultValue("20101010")
    @PropertyName(name = "dyno.backup.restore.date")
    public String getRestoreDate();
    

    
    
}
