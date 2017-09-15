package com.netflix.nfsidecar.config;

import java.util.List;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "florida.config")
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
    @DefaultValue("8101") //TODO: For a common default value we probably have to result to defined FP
    public int getStoragePeerPort();
    

    @DefaultValue("false")
    @PropertyName(name = "dyno.backup.snapshot.enabled") //TODO: For a common default value we probably have to result to defined FP
    public boolean isBackupEnabled();

    @DefaultValue("false")
    @PropertyName(name = "dyno.backup.restore.enabled") //TODO: For a common default value we probably have to result to defined FP
    public boolean isRestoreEnabled();

    @DefaultValue("day")
    @PropertyName(name = "dyno.backup.schedule") //TODO: For a common default value we probably have to result to defined FP
    public String getBackupSchedule();

    @DefaultValue("12")
    @PropertyName(name = "dyno.backup.hour") //TODO: For a common default value we probably have to result to defined FP
    public int getBackupHour();

    @DefaultValue("20101010")
    @PropertyName(name = "dyno.backup.restore.date")
    public String getRestoreDate();
    

    
    
}
