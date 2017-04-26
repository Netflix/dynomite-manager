/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.identity;

import java.util.*;

import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.*;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import com.netflix.astyanax.util.TimeUUIDUtils;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.supplier.HostSupplier;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InstanceDataDAOCassandra {
	private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAOCassandra.class);

	private String CN_ID = "Id";
	private String CN_APPID = "appId";
	private String CN_AZ = "availabilityZone";
	private String CN_DC = "datacenter";
	private String CN_INSTANCEID = "instanceId";
	private String CN_HOSTNAME = "hostname";
	private String CN_EIP = "elasticIP";
	private String CN_TOKEN = "token";
	private String CN_LOCATION = "location";
	private String CN_VOLUME_PREFIX = "ssVolumes";
	private String CN_UPDATETIME = "updatetime";
	private String CF_NAME_TOKENS = "tokens";
	private String CF_NAME_LOCKS = "locks";

	private final Keyspace bootKeyspace;
	private final IConfiguration config;
	private final HostSupplier hostSupplier;
	private final String BOOT_CLUSTER;
	private final String KS_NAME;
	private final int thriftPortForAstyanax;
	private final AstyanaxContext<Keyspace> ctx;

	/*
	 * Schema: create column family tokens with comparator=UTF8Type and
	 * column_metadata=[ {column_name: appId, validation_class:
	 * UTF8Type,index_type: KEYS}, {column_name: instanceId, validation_class:
	 * UTF8Type}, {column_name: token, validation_class: UTF8Type},
	 * {column_name: availabilityZone, validation_class: UTF8Type},
	 * {column_name: hostname, validation_class: UTF8Type},{column_name: Id,
	 * validation_class: UTF8Type}, {column_name: elasticIP, validation_class:
	 * UTF8Type}, {column_name: updatetime, validation_class: TimeUUIDType},
	 * {column_name: location, validation_class: UTF8Type}];
	 */
	public ColumnFamily<String, String> CF_TOKENS = new ColumnFamily<String, String>(CF_NAME_TOKENS,
			StringSerializer.get(), StringSerializer.get());
	// Schema: create column family locks with comparator=UTF8Type;
	public ColumnFamily<String, String> CF_LOCKS = new ColumnFamily<String, String>(CF_NAME_LOCKS,
			StringSerializer.get(), StringSerializer.get());

	@Inject
	public InstanceDataDAOCassandra(IConfiguration config, HostSupplier hostSupplier) throws ConnectionException {
		this.config = config;

		BOOT_CLUSTER = config.getCassandraClusterName();

		if (BOOT_CLUSTER == null || BOOT_CLUSTER.isEmpty())
			throw new RuntimeException(
					"Cassandra cluster name cannot be blank. Please use getCassandraClusterName() property.");

		KS_NAME = config.getCassandraKeyspaceName();

		if (KS_NAME == null || KS_NAME.isEmpty())
			throw new RuntimeException(
					"Cassandra Keyspace can not be blank. Please use getCassandraKeyspaceName() property.");

		thriftPortForAstyanax = config.getCassandraThriftPort();
		if (thriftPortForAstyanax <= 0)
			throw new RuntimeException(
					"Thrift Port for Astyanax can not be blank. Please use getCassandraThriftPort() property.");

		this.hostSupplier = hostSupplier;

		if (config.isEurekaHostsSupplierEnabled())
			ctx = initWithThriftDriverWithEurekaHostsSupplier();
		else
			ctx = initWithThriftDriverWithExternalHostsSupplier();

		ctx.start();
		bootKeyspace = ctx.getClient();
	}

	public void createInstanceEntry(AppsInstance instance) throws Exception {
		logger.info("*** Creating New Instance Entry ***");
		
		String key = getRowKey(instance);
		logger.info("KEY fronm CASS: {}",new Object[]{key});
		
		String uniqueID = InstanceIdentityUniqueGenerator.createUniqueID(instance.getInstanceId());
		logger.info("*** Checking for Instances with app: {}, id: {}, rackName {} ",new Object[]{instance.getApp(),uniqueID,instance.getRack()});
		
		if (getInstance(instance.getApp(), config.getRack(), instance.getId()) != null){
			logger.info("*** Not inserting new data into Cassandra. ***");
			return;
		}

		logger.info("*** Will insert new data into Cassandra. ***");
		getLock(instance);

		try {
			MutationBatch m = bootKeyspace.prepareMutationBatch();
			ColumnListMutation<String> clm = m.withRow(CF_TOKENS, key);
			clm.putColumn(CN_ID, uniqueID, null);
			clm.putColumn(CN_APPID, instance.getApp(), null);
			clm.putColumn(CN_AZ, instance.getZone(), null);
			clm.putColumn(CN_DC, config.getRack(), null);
			clm.putColumn(CN_INSTANCEID, instance.getInstanceId(), null);
			clm.putColumn(CN_HOSTNAME, instance.getHostName(), null);
			clm.putColumn(CN_EIP, instance.getHostIP(), null);
			clm.putColumn(CN_TOKEN, instance.getToken(), null);
			clm.putColumn(CN_LOCATION, instance.getDatacenter(), null);
			clm.putColumn(CN_UPDATETIME, TimeUUIDUtils.getUniqueTimeUUIDinMicros(), null);
			Map<String, Object> volumes = instance.getVolumes();
			if (volumes != null) {
				for (String path : volumes.keySet()) {
					clm.putColumn(CN_VOLUME_PREFIX + "_" + path, volumes.get(path).toString(),
							null);
				}
			}
			m.execute();
			logger.info(String.format("Key %s INSERTED on CASS", key));
		} catch (Exception e) {
			logger.info(e.getMessage());
		} finally {
			releaseLock(instance);
		}
	}

	/*
	 * To get a lock on the row - Create a choosing row and make sure there are
	 * no contenders. If there are bail out. Also delete the column when bailing
	 * out. - Once there are no contenders, grab the lock if it is not already
	 * taken.
	 */
	private void getLock(AppsInstance instance) throws Exception {

		String choosingkey = getChoosingKey(instance);
		MutationBatch m = bootKeyspace.prepareMutationBatch();
		ColumnListMutation<String> clm = m.withRow(CF_LOCKS, choosingkey);

		// Expire in 6 sec
		clm.putColumn(instance.getInstanceId(), instance.getInstanceId(), new Integer(6));
		m.execute();
		int count = bootKeyspace.prepareQuery(CF_LOCKS).getKey(choosingkey).getCount().execute().getResult();
		if (count > 1) {
			// Need to delete my entry
			m.withRow(CF_LOCKS, choosingkey).deleteColumn(instance.getInstanceId());
			m.execute();
			throw new Exception(String.format("More than 1 contender for lock %s %d", choosingkey, count));
		}

		String lockKey = getLockingKey(instance);
		OperationResult<ColumnList<String>> result = bootKeyspace.prepareQuery(CF_LOCKS).getKey(lockKey)
				.execute();
		if (result.getResult().size() > 0 && !result.getResult().getColumnByIndex(0).getName()
				.equals(instance.getInstanceId()))
			throw new Exception(String.format("Lock already taken %s", lockKey));

		clm = m.withRow(CF_LOCKS, lockKey);
		clm.putColumn(instance.getInstanceId(), instance.getInstanceId(), new Integer(600));
		m.execute();
		Thread.sleep(100);
		result = bootKeyspace.prepareQuery(CF_LOCKS).getKey(lockKey).execute();
		if (result.getResult().size() == 1 && result.getResult().getColumnByIndex(0).getName()
				.equals(instance.getInstanceId())) {
			logger.info("Got lock " + lockKey);
			return;
		} else
			throw new Exception(String.format("Cannot insert lock %s", lockKey));

	}

	private void releaseLock(AppsInstance instance) throws Exception {
		String choosingkey = getChoosingKey(instance);
		MutationBatch m = bootKeyspace.prepareMutationBatch();
		ColumnListMutation<String> clm = m.withRow(CF_LOCKS, choosingkey);

		m.withRow(CF_LOCKS, choosingkey).deleteColumn(instance.getInstanceId());
		m.execute();
	}

	public void deleteInstanceEntry(AppsInstance instance) throws Exception {
		logger.info("deleteInstanceEntry(). Found Dead node trying to DELETE it {}", new Object[]{instance});
		// Acquire the lock first
		getLock(instance);

		// Delete the row
		String uniqueID = InstanceIdentityUniqueGenerator.createUniqueID(instance.getInstanceId());
		String key = findKey(instance.getApp(), uniqueID, instance.getDatacenter(),instance.getRack());
		
		if (key == null){
			logger.info("Key not found - no delete - app: {}, id: {}, DC: {},  rack: {}", new Object[]{instance.getApp(), String.valueOf(instance.getId()), instance.getDatacenter(),instance.getRack()});
			return;
		}
		
		MutationBatch m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_TOKENS, key).delete();
		m.execute();

		key = getLockingKey(instance);
		// Delete key
		m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_LOCKS, key).delete();
		m.execute();

		// Have to delete choosing key as well to avoid issues with delete
		// followed by immediate writes
		key = getChoosingKey(instance);
		m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_LOCKS, key).delete();
		m.execute();
		
		logger.info("deleteInstanceEntry(). DELETED! ");
	}

	public AppsInstance getInstance(String app, String rack, String id) {
		logger.info("Listing  instances in Cassandra... ");
		Set<AppsInstance> set = getAllInstances(app);
		
		for (AppsInstance ins : set) {
			logger.info("Instance ID:{} - RACK:{} ", new Object[]{ins.getId(),ins.getRack()});
			if (id.equals(ins.getId()) && rack.equals(ins.getRack())){
				return ins;	
			}
		}
		return null;
	}

	public Set<AppsInstance> getLocalDCInstances(String app, String region) {
		Set<AppsInstance> set = getAllInstances(app);
		Set<AppsInstance> returnSet = new HashSet<AppsInstance>();

		for (AppsInstance ins : set) {
			if (ins.getDatacenter().equals(region))
				returnSet.add(ins);
		}
		return returnSet;
	}

	public Set<AppsInstance> getAllInstances(String app) {
		Set<AppsInstance> set = new HashSet<AppsInstance>();
		try {

			final String selectClause = String
					.format("SELECT * FROM %s USING CONSISTENCY LOCAL_QUORUM WHERE %s = '%s' ",
							CF_NAME_TOKENS, CN_APPID, app);
			logger.debug(selectClause);

			final ColumnFamily<String, String> CF_TOKENS_NEW = ColumnFamily
					.newColumnFamily(KS_NAME, StringSerializer.get(), StringSerializer.get());

			OperationResult<CqlResult<String, String>> result = bootKeyspace.prepareQuery(CF_TOKENS_NEW)
					.withCql(selectClause).execute();

			for (Row<String, String> row : result.getResult().getRows())
				set.add(transform(row.getColumns()));
		} catch (Exception e) {
			logger.warn("Caught an Unknown Exception during reading msgs ... -> " + e.getMessage());
			throw new RuntimeException(e);
		}
		return set;
	}

	public String findKey(String app, String id, String location, String datacenter) {
		try {
			final String selectClause = String
					.format("SELECT * FROM %s USING CONSISTENCY LOCAL_QUORUM WHERE %s = '%s' and %s = '%s' and %s = '%s' and %s = '%s' ",
							"tokens", CN_APPID, app, CN_ID, id, CN_LOCATION, location,
							CN_DC, datacenter);
			logger.info(selectClause);

			final ColumnFamily<String, String> CF_INSTANCES_NEW = ColumnFamily
					.newColumnFamily(KS_NAME, StringSerializer.get(), StringSerializer.get());

			OperationResult<CqlResult<String, String>> result = bootKeyspace.prepareQuery(CF_INSTANCES_NEW)
					.withCql(selectClause).execute();

			if (result == null || result.getResult().getRows().size() == 0)
				return null;

			Row<String, String> row = result.getResult().getRows().getRowByIndex(0);
			return row.getKey();

		} catch (Exception e) {
			logger.warn("Caught an Unknown Exception during find a row matching cluster[" + app +
					"], id[" + id + "], and region[" + datacenter + "]  ... -> " + e.getMessage());
			throw new RuntimeException(e);
		}

	}

	private AppsInstance transform(ColumnList<String> columns) {
		AppsInstance ins = new AppsInstance();
		Map<String, String> cmap = new HashMap<String, String>();
		for (Column<String> column : columns) {
			//        		logger.info("***Column Name = "+column.getName()+ " Value = "+column.getStringValue());
			cmap.put(column.getName(), column.getStringValue());
			if (column.getName().equals(CN_APPID))
				ins.setUpdatetime(column.getTimestamp());
		}

		ins.setApp(cmap.get(CN_APPID));
		ins.setZone(cmap.get(CN_AZ));
		ins.setHost(cmap.get(CN_HOSTNAME));
		ins.setHostIP(cmap.get(CN_EIP));
		ins.setId(cmap.get(CN_ID));
		ins.setInstanceId(cmap.get(CN_INSTANCEID));
		ins.setDatacenter(cmap.get(CN_LOCATION));
		ins.setRack(cmap.get(CN_DC));
		ins.setToken(cmap.get(CN_TOKEN));
		return ins;
	}

	private String getChoosingKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId() + "-choosing";
	}

	private String getLockingKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId() + "-lock";
	}

	private String getRowKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId();
	}

	private AstyanaxContext<Keyspace> initWithThriftDriverWithEurekaHostsSupplier() {

		logger.info("BOOT_CLUSTER = {}, KS_NAME = {}", BOOT_CLUSTER, KS_NAME);
		return new AstyanaxContext.Builder().forCluster(BOOT_CLUSTER).forKeyspace(KS_NAME)
				.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
						.setDiscoveryType(NodeDiscoveryType.DISCOVERY_SERVICE))
				.withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
						.setMaxConnsPerHost(3).setPort(thriftPortForAstyanax))
				.withHostSupplier(hostSupplier.getSupplier(BOOT_CLUSTER))
				.withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
				.buildKeyspace(ThriftFamilyFactory.getInstance());

	}

	private AstyanaxContext<Keyspace> initWithThriftDriverWithExternalHostsSupplier() {

		logger.info("BOOT_CLUSTER = {}, KS_NAME = {}", BOOT_CLUSTER, KS_NAME);
		return new AstyanaxContext.Builder().forCluster(BOOT_CLUSTER).forKeyspace(KS_NAME)
				.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
						.setDiscoveryType(NodeDiscoveryType.DISCOVERY_SERVICE)
						.setConnectionPoolType(ConnectionPoolType.ROUND_ROBIN))
				.withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
						.setMaxConnsPerHost(3).setPort(thriftPortForAstyanax))
				.withHostSupplier(getSupplier())
				.withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
				.buildKeyspace(ThriftFamilyFactory.getInstance());

	}

	private Supplier<List<Host>> getSupplier() {

		return new Supplier<List<Host>>() {

			@Override
			public List<Host> get() {

				List<Host> hosts = new ArrayList<Host>();

				List<String> cassHostnames = new ArrayList<String>(Arrays.asList(StringUtils
						.split(config.getCassandraSeeds(), ",")));

				if (cassHostnames.size() == 0)
					throw new RuntimeException(
							"Cassandra Host Names can not be blank. At least one host is needed. Please use getCassandraSeeds() property.");

				for (String cassHost : cassHostnames) {
					logger.info("Adding Cassandra Host = {}", cassHost);
					hosts.add(new Host(cassHost, thriftPortForAstyanax));
				}

				return hosts;
			}
		};
	}
}
