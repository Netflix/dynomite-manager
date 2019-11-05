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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.nfsidecar.identity.AppsInstance;
import com.netflix.nfsidecar.resources.env.IEnvVariables;
import com.netflix.nfsidecar.tokensdb.IAppsInstanceFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class DynamoDBInstanceFactory implements IAppsInstanceFactory {

    private final InstanceDataDAODynamoDB instanceDataDAODynamoDB;
    private final IEnvVariables envVariables;

    @Inject
    public DynamoDBInstanceFactory(InstanceDataDAODynamoDB instanceDataDAODynamoDB, IEnvVariables envVariables) {
        this.instanceDataDAODynamoDB = instanceDataDAODynamoDB;
        this.envVariables = envVariables;
    }

    @Override
    public List<AppsInstance> getAllIds(String appName) {
        return instanceDataDAODynamoDB.getAllInstances(appName)
                .stream()
                .map(this::convertToAppInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppsInstance> getLocalDCIds(String appName, String region) {
        return instanceDataDAODynamoDB.getAllInstances(appName, region)
                .stream()
                .map(this::convertToAppInstance)
                .collect(Collectors.toList());
    }

    @Override
    public AppsInstance getInstance(String appName, String dc, int id) {
        return convertToAppInstance(instanceDataDAODynamoDB.getInstance(appName, dc, id));
    }

    @Override
    public AppsInstance create(String app, int id, String instanceID, String hostname, String ip, String zone, Map<String, Object> volumes, String payload, String rack) {

        DynamoDBAppInstance dynamoDBAppInstance = new DynamoDBAppInstance();

        dynamoDBAppInstance.setKey(getKey(app, rack, id));
        dynamoDBAppInstance.setApp(app);
        dynamoDBAppInstance.setAvailabilityZone(zone);
        dynamoDBAppInstance.setId(id);
        dynamoDBAppInstance.setInstanceId(instanceID);
        dynamoDBAppInstance.setHostname(hostname);
        dynamoDBAppInstance.setPublicIp(ip);
        dynamoDBAppInstance.setRack(rack);
        dynamoDBAppInstance.setToken(payload);
        dynamoDBAppInstance.setLocation(envVariables.getRegion());

        instanceDataDAODynamoDB.createInstance(dynamoDBAppInstance);

        return convertToAppInstance(dynamoDBAppInstance);
    }

    @Override
    public void delete(AppsInstance appsInstance) {
        DynamoDBAppInstance dynamoDBAppInstance = new DynamoDBAppInstance();
        dynamoDBAppInstance.setKey(getKey(appsInstance.getApp(), appsInstance.getRack(), appsInstance.getId()));

        try {
            instanceDataDAODynamoDB.deleteInstance(dynamoDBAppInstance);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(AppsInstance inst) {
        // Looks like no one is using this method.
        // This method is not supported as of now.

        throw new UnsupportedOperationException("Update operation on DynamoDB for AppsInstance is not supported");
    }

    @Override
    public void sort(List<AppsInstance> appsInstances) {
//        Comparator<? super AppsInstance> comparator = new Comparator<AppsInstance>()
//        {
//            @Override
//            public int compare(AppsInstance o1, AppsInstance o2)
//            {
//                Integer c1 = o1.getId();
//                Integer c2 = o2.getId();
//                return c1.compareTo(c2);
//            }
//        };
//        Collections.sort(appsInstances, comparator);
        appsInstances.sort(Comparator.comparing(AppsInstance::getId));
    }

    @Override
    public void attachVolumes(AppsInstance instance, String mountPath, String device) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    private String getKey(String app, String rack, int id) {
        return app + "_" + rack + "_" + id;
    }

    private AppsInstance convertToAppInstance(DynamoDBAppInstance dynamoDBAppInstance) {

        if (dynamoDBAppInstance == null)
            return null;

        AppsInstance appsInstance = new AppsInstance();

        appsInstance.setApp(dynamoDBAppInstance.getApp());
        appsInstance.setDatacenter(dynamoDBAppInstance.getLocation());
        appsInstance.setHost(dynamoDBAppInstance.getHostname());
        appsInstance.setHostIP(dynamoDBAppInstance.getPublicIp());
        appsInstance.setId(dynamoDBAppInstance.getId());
        appsInstance.setInstanceId(dynamoDBAppInstance.getInstanceId());
        appsInstance.setRack(dynamoDBAppInstance.getRack());
        appsInstance.setToken(dynamoDBAppInstance.getToken());
        appsInstance.setZone(dynamoDBAppInstance.getAvailabilityZone());
        appsInstance.setUpdatetime(dynamoDBAppInstance.getLastUpdatedTime());

        return appsInstance;
    }

}
