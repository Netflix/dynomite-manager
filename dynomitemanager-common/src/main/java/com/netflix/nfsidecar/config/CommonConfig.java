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
     * @return Bootstrap cluster name (depends on another cass cluster)
     */
    @DefaultValue("cass_turtle")
    @PropertyName(name = "dyno.sidecore.clusterName")
    public String getCassandraClusterName();
    
    /**
     * @return if Eureka is used to find the bootstrap cluster
     */
    @DefaultValue("false")
    @PropertyName(name = "dyno.sidecore.eureka.enabled")
    public boolean isEurekaHostsSupplierEnabled();
    
    /**
     * @return the port that the bootstrap cluster can be contacted
     */
    @DefaultValue("7102")
    @PropertyName(name = "dyno.sidecore.sidecore.port")
    public int getCassandraThriftPort();
    
    @DefaultValue("127.0.0.1")
    @PropertyName(name = "dyno.sidecore.seeds")
    public String getCassandraSeeds();
    
    /**
     * Get the name of the keyspace that stores tokens for the Dynomite cluster.
     *
     * @return the keyspace name
     */
    @DefaultValue("dyno_bootstrap")
    @PropertyName(name = "metadata.keyspace")
    public String getCassandraKeyspaceName();
    
    /**
     * Get the peer-to-peer port used by Dynomite to communicate with other
     * Dynomite nodes.
     *
     * @return the peer-to-peer port used for intra-cluster communication
     */
    @DefaultValue("8101") //TODO: For a common default value we probably have to result to defined FP
    public int getStoragePeerPort();
    

    // Dual Account
    /*
     * @return the Amazon Resource Name (ARN) for EC2 classic.
     */
    @DefaultValue("null")
    @PropertyName(name = "ec2.roleassumption.arn")
    public String getClassicAWSRoleAssumptionArn();

    /*
     * @return the Amazon Resource Name (ARN) for VPC.
     */
    @DefaultValue("null")
    @PropertyName(name = "vpc.roleassumption.arn")
    public String getVpcAWSRoleAssumptionArn();

    @DefaultValue("false")
    @PropertyName(name = "roleassumption.dualaccount")
    public boolean isDualAccount();

    // Backup and Restore

    @DefaultValue("us-east-1.dynomite-backup-test")
    @PropertyName(name = "dyno.backup.bucket.name") //TODO: For a common default value we probably have to result to defined FP
    public String getBucketName();

    @DefaultValue("backup")
    @PropertyName(name = "dyno.backup.s3.base_dir") //TODO: For a common default value we probably have to result to defined FP
    public String getBackupLocation();

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
