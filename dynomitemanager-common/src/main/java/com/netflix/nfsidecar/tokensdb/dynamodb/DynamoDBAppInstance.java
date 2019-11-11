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
package com.netflix.nfsidecar.tokensdb.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "app_instance")
public class DynamoDBAppInstance {

    static final String APP_INSTANCE_APP_RACK_GSI = "app_instance-app-rack-gsi";
    static final String APP_INSTANCE_APP_LOCATION_GSI = "app_instance-app-location-gsi";

    @DynamoDBHashKey(attributeName = "key")
    private String key;

    @DynamoDBAttribute
    @DynamoDBIndexHashKey(globalSecondaryIndexNames = {APP_INSTANCE_APP_RACK_GSI, APP_INSTANCE_APP_LOCATION_GSI})
    private String app;

    @DynamoDBAttribute
    private String availabilityZone;

    //dc equivalent in cassandra
    @DynamoDBAttribute
    @DynamoDBIndexRangeKey(globalSecondaryIndexNames = {APP_INSTANCE_APP_RACK_GSI})
    private String rack;

    @DynamoDBAttribute
    private String instanceId;

    @DynamoDBAttribute
    private String hostname;

    //Equivalent to data center in cassandra
    @DynamoDBAttribute
    private String publicIp;

    @DynamoDBAttribute
    private String token;

    //Equivalent to data center in cassandra
    @DynamoDBAttribute
    @DynamoDBIndexRangeKey(globalSecondaryIndexNames = {APP_INSTANCE_APP_LOCATION_GSI})
    private String location;

    @DynamoDBAttribute
    private int peerPort;

    @DynamoDBAttribute
    private int Id;

    @DynamoDBAttribute
    @DynamoDBAutoGeneratedTimestamp
    private long lastUpdatedTime;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(int peerPort) {
        this.peerPort = peerPort;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(long lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }
}