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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.nfsidecar.config.DynamoDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import static com.netflix.nfsidecar.tokensdb.dynamodb.DynamoDBAppInstance.APP_INSTANCE_APP_LOCATION_GSI;
import static com.netflix.nfsidecar.tokensdb.dynamodb.DynamoDBAppInstance.APP_INSTANCE_APP_RACK_GSI;

@Singleton
public class InstanceDataDAODynamoDB {

    private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAODynamoDB.class);

    private final DynamoDBMapper dynamoDBMapper;
    private final DynamoDBSaveExpression saveExpression;
    private final DynamoDBConfig dynamoDBConfig;

    @Inject
    public InstanceDataDAODynamoDB(DynamoDBConfig dynamoDBConfig) {
        this.dynamoDBConfig = dynamoDBConfig;

        AmazonDynamoDBClientBuilder amazonDynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard();
        amazonDynamoDBClientBuilder.setCredentials(new DefaultAWSCredentialsProviderChain());

        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBClientBuilder.build();
        TableNameOverride tableNameOverride = TableNameOverride.withTableNamePrefix(dynamoDBConfig.getDynamoDBTableNamePrefix());

        DynamoDBMapperConfig.Builder builder = DynamoDBMapperConfig.builder();
        builder.setTableNameOverride(tableNameOverride);
        DynamoDBMapperConfig dynamoDBMapperConfig = builder.build();

        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        saveExpression = new DynamoDBSaveExpression();
        Map<String, ExpectedAttributeValue> saveCondition = new HashMap<>();
        saveCondition.put("key", new ExpectedAttributeValue().withExists(false));
        saveExpression.setExpected(saveCondition);
    }

    public void createInstance(DynamoDBAppInstance dynamoDBAppInstance) {
        dynamoDBMapper.save(dynamoDBAppInstance, saveExpression);
    }

    public void deleteInstance(DynamoDBAppInstance dynamoDBAppInstance) {
        dynamoDBMapper.delete(dynamoDBAppInstance);
    }

    public Set<DynamoDBAppInstance> getAllInstances(String app) {
        DynamoDBAppInstance dynamoDBAppInstance = new DynamoDBAppInstance();
        dynamoDBAppInstance.setApp(app);

        DynamoDBQueryExpression<DynamoDBAppInstance> dynamoDBQueryExpression = new DynamoDBQueryExpression<DynamoDBAppInstance>()
                .withIndexName(APP_INSTANCE_APP_RACK_GSI)
                .withHashKeyValues(dynamoDBAppInstance)
                .withConsistentRead(false);

        PaginatedQueryList<DynamoDBAppInstance> paginatedQueryList = dynamoDBMapper.query(DynamoDBAppInstance.class, dynamoDBQueryExpression);

        return paginatedQueryList.parallelStream().collect(Collectors.toSet());
    }

    public Set<DynamoDBAppInstance> getAllInstances(String app, String region) {
        DynamoDBAppInstance dynamoDBAppInstance = new DynamoDBAppInstance();
        dynamoDBAppInstance.setApp(app);

        Condition condition = new Condition();
        AttributeValue attributeValue = new AttributeValue().withS(region);
        condition.withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(attributeValue);

        DynamoDBQueryExpression<DynamoDBAppInstance> dynamoDBQueryExpression = new DynamoDBQueryExpression<DynamoDBAppInstance>()
                .withIndexName(APP_INSTANCE_APP_LOCATION_GSI)
                .withHashKeyValues(dynamoDBAppInstance)
                .withRangeKeyCondition("location", condition)
                .withConsistentRead(false);

        PaginatedQueryList<DynamoDBAppInstance> paginatedQueryList = dynamoDBMapper.query(DynamoDBAppInstance.class, dynamoDBQueryExpression);

        return paginatedQueryList.parallelStream().collect(Collectors.toSet());
    }

    public DynamoDBAppInstance getInstance(String app, String rack, int id) {
        DynamoDBAppInstance dynamoDBAppInstance = new DynamoDBAppInstance();
        dynamoDBAppInstance.setApp(app);

        Condition condition = new Condition();
        AttributeValue attributeValue = new AttributeValue().withS(rack);
        condition.withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(attributeValue);

        DynamoDBQueryExpression<DynamoDBAppInstance> dynamoDBQueryExpression = new DynamoDBQueryExpression<DynamoDBAppInstance>()
                .withIndexName(APP_INSTANCE_APP_RACK_GSI)
                .withHashKeyValues(dynamoDBAppInstance)
                .withRangeKeyCondition("rack", condition)
                .withConsistentRead(false);

        PaginatedQueryList<DynamoDBAppInstance> paginatedQueryList = dynamoDBMapper.query(DynamoDBAppInstance.class, dynamoDBQueryExpression);

        return paginatedQueryList.parallelStream().filter(appInstance -> appInstance.getId() == id).findFirst().orElse(null);
    }


}
