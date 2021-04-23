package com.netflix.nfsidecar.tokensdb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.nfsidecar.config.CassCommonConfig;
import com.netflix.nfsidecar.config.CommonConfig;
import com.netflix.nfsidecar.identity.AppsInstance;
import com.netflix.nfsidecar.supplier.Host;
import com.netflix.nfsidecar.supplier.HostSupplier;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.now;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.update;

@Singleton
public class InstanceDataDAOCassandra {
    private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAOCassandra.class);

    private static final String CN_KEY = "key";
    private static final String CN_ID = "Id";
    private static final String CN_APPID = "appId";
    private static final String CN_AZ = "availabilityZone";
    private static final String CN_DC = "datacenter";
    private static final String CN_INSTANCEID = "instanceId";
    private static final String CN_HOSTNAME = "hostname";
    private static final String CN_DYNOMITE_PORT = "dynomitePort";
    private static final String CN_DYNOMITE_SECURE_PORT = "dynomiteSecurePort";
    private static final String CN_DYNOMITE_SECURE_STORAGE_PORT = "dynomiteSecureStoragePort";
    private static final String CN_PEER_PORT = "peerPort";
    private static final String CN_EIP = "elasticIP";
    private static final String CN_TOKEN = "token";
    private static final String CN_LOCATION = "location";
    private static final String CN_VOLUMES = "ssVolumes";
    private static final String CN_UPDATETIME = "updatetime";
    private static final String CF_NAME_TOKENS = "tokens";
    private static final String CF_NAME_LOCKS = "locks";

    private final CqlSession bootSession;
    private final CommonConfig commonConfig;
    private final CassCommonConfig cassCommonConfig;
    private final HostSupplier hostSupplier;
    private final String BOOT_CLUSTER;
    private final String KS_NAME;
    private final int cassandraPort;
    private long lastTimeCassandraPull = 0;
    private Set<AppsInstance> appInstances;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock read  = readWriteLock.readLock();
    private final Lock write = readWriteLock.writeLock();

    @Inject
    public InstanceDataDAOCassandra(CommonConfig commonConfig, CassCommonConfig cassCommonConfig, HostSupplier hostSupplier) {
        this.cassCommonConfig = cassCommonConfig;
        this.commonConfig = commonConfig;

        BOOT_CLUSTER = cassCommonConfig.getCassandraClusterName();

        if (BOOT_CLUSTER == null || BOOT_CLUSTER.isEmpty())
            throw new RuntimeException(
                    "Cassandra cluster name cannot be blank. Please use getCassandraClusterName() property.");

        KS_NAME = cassCommonConfig.getCassandraKeyspaceName();

        if (KS_NAME == null || KS_NAME.isEmpty())
            throw new RuntimeException(
                    "Cassandra Keyspace can not be blank. Please use getCassandraKeyspaceName() property.");

        cassandraPort = cassCommonConfig.getCassandraPort();
        if (cassandraPort <= 0)
            throw new RuntimeException(
                    "Thrift Port for Astyanax can not be blank. Please use getCassandraThriftPort() property.");

        this.hostSupplier = hostSupplier;

        this.bootSession = init();
    }
    private boolean isCassandraCacheExpired() {
        if (lastTimeCassandraPull + cassCommonConfig.getTokenRefreshInterval() <= System.currentTimeMillis())
            return true;
        return false;
    }

    public void createInstanceEntry(AppsInstance instance) throws Exception {
        logger.info("*** Creating New Instance Entry ***");
        String key = getRowKey(instance);
        // If the key exists throw exception
        if (getInstance(instance.getApp(), instance.getRack(), instance.getId()) != null) {
            logger.info(String.format("Key already exists: %s", key));
            return;
        }

        getLock(instance);

        try {
            this.bootSession.execute(
                    insertInto(CF_NAME_TOKENS)
                            .value(CN_KEY, literal(key))
                            .value(CN_ID, literal(String.valueOf(instance.getId())))
                            .value(CN_APPID, literal(instance.getApp()))
                            .value(CN_AZ, literal(instance.getZone()))
                            .value(CN_DC, literal(instance.getDatacenter()))
                            .value(CN_LOCATION, literal(commonConfig.getRack()))
                            .value(CN_INSTANCEID, literal(instance.getInstanceId()))
                            .value(CN_HOSTNAME, literal(instance.getHostName()))
                            .value(CN_DYNOMITE_PORT, literal(instance.getDynomitePort()))
                            .value(CN_DYNOMITE_SECURE_PORT, literal(instance.getDynomiteSecurePort()))
                            .value(CN_DYNOMITE_SECURE_STORAGE_PORT, literal(instance.getDynomiteSecureStoragePort()))
                            .value(CN_PEER_PORT, literal(instance.getPeerPort()))
                            .value(CN_EIP, literal(instance.getHostIP()))
                            // 'token' is a reserved name in cassandra, so it needs to be double-quoted
                            .value("\"" + CN_TOKEN + "\"", literal(instance.getToken()))
                            .value(CN_VOLUMES, literal(formatVolumes(instance.getVolumes())))
                            .value(CN_UPDATETIME, now())
                            .build()
            );
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            releaseLock(instance);
        }
    }

    public void updateInstanceEntry(final AppsInstance instance) throws Exception {
        logger.info("*** Updating Instance Entry ***");
        String key = getRowKey(instance);
        if (getInstance(instance.getApp(), instance.getRack(), instance.getId()) == null) {
            logger.info(String.format("Key doesn't exist: %s", key));
            createInstanceEntry(instance);
            return;
        }

        getLock(instance);

        try {
            this.bootSession.execute(
                    update(CF_NAME_TOKENS)
                            .setColumn(CN_ID, literal(String.valueOf(instance.getId())))
                            .setColumn(CN_APPID, literal(instance.getApp()))
                            .setColumn(CN_AZ, literal(instance.getZone()))
                            .setColumn(CN_DC, literal(instance.getDatacenter()))
                            .setColumn(CN_LOCATION, literal(commonConfig.getRack()))
                            .setColumn(CN_INSTANCEID, literal(instance.getInstanceId()))
                            .setColumn(CN_HOSTNAME, literal(instance.getHostName()))
                            .setColumn(CN_DYNOMITE_PORT, literal(instance.getDynomitePort()))
                            .setColumn(CN_DYNOMITE_SECURE_PORT, literal(instance.getDynomiteSecurePort()))
                            .setColumn(CN_DYNOMITE_SECURE_STORAGE_PORT, literal(instance.getDynomiteSecureStoragePort()))
                            .setColumn(CN_PEER_PORT, literal(instance.getPeerPort()))
                            .setColumn(CN_EIP, literal(instance.getHostIP()))
                            // 'token' is a reserved name in cassandra, so it needs to be double-quoted
                            .setColumn("\"" + CN_TOKEN + "\"", literal(instance.getToken()))
                            .setColumn(CN_VOLUMES, literal(formatVolumes(instance.getVolumes())))
                            .setColumn(CN_UPDATETIME, now())
                            .whereColumn(CN_KEY).isEqualTo(literal(key))
                            .build()
            );
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            releaseLock(instance);
        }
    }

    private Map<String, String> formatVolumes(final Map<String, Object> volumes) {
        if (volumes == null) {
            return Collections.emptyMap();
        } else {
            return volumes.entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private List<Row> fetchRows(final String table, final String keyColumn, final String key) {
        return this.bootSession.execute(
                selectFrom(table)
                        .all()
                        .whereColumn(keyColumn).isEqualTo(literal(key))
                        .build()
        ).all();
    }

    private long rowCount(final String table, final String keyColumn, final String key) {
        final Row row = this.bootSession.execute(
                selectFrom(table)
                        .countAll()
                        .whereColumn(keyColumn).isEqualTo(literal(key))
                        .build()
        ).one();
        return row != null ? row.getLong(0) : 0;
    }

    /*
     * To get a lock on the row - Create a choosing row and make sure there are
     * no contenders. If there are bail out. Also delete the column when bailing
     * out. - Once there are no contenders, grab the lock if it is not already
     * taken.
     */
    private void getLock(AppsInstance instance) throws Exception {
        final String choosingKey = getChoosingKey(instance);

        this.bootSession.execute(
                insertInto(CF_NAME_LOCKS)
                        .value(CN_KEY, literal(choosingKey))
                        .value(CN_INSTANCEID, literal(instance.getInstanceId()))
                        .usingTtl(6)
                        .build()
        );
        final long count = rowCount(CF_NAME_LOCKS, CN_KEY, choosingKey);
        if (count > 1) {
            // Need to delete my entry
            this.bootSession.execute(
                    deleteFrom(CF_NAME_LOCKS)
                            .whereColumn(CN_KEY).isEqualTo(literal(choosingKey))
                            .whereColumn(CN_INSTANCEID).isEqualTo(literal(instance.getInstanceId()))
                            .build()
            );
            throw new Exception(String.format("More than 1 contender for lock %s %d", choosingKey, count));
        }

        final String lockKey = getLockingKey(instance);
        final List<Row> preCheck = fetchRows(CF_NAME_LOCKS, CN_KEY, lockKey);
        if (preCheck.size() > 0 && preCheck.stream().noneMatch(row -> instance.getInstanceId().equals(row.getString(CN_INSTANCEID)))) {
            throw new Exception(String.format("Lock already taken %s", lockKey));
        }

        this.bootSession.execute(
                insertInto(CF_NAME_LOCKS)
                        .value(CN_KEY, literal(lockKey))
                        .value(CN_INSTANCEID, literal(instance.getInstanceId()))
                        .usingTtl(600)
                        .build()
        );
        Thread.sleep(100);
        final List<Row> postCheck = fetchRows(CF_NAME_LOCKS, CN_KEY, lockKey);
        if (postCheck.size() == 1 && instance.getInstanceId().equals(postCheck.get(0).getString(CN_INSTANCEID))) {
            logger.info("Got lock " + lockKey);
        } else {
            throw new Exception(String.format("Cannot insert lock %s", lockKey));
        }
    }

    private void releaseLock(AppsInstance instance) throws Exception {
        final String choosingKey = getChoosingKey(instance);

        this.bootSession.execute(
                deleteFrom(CF_NAME_LOCKS)
                        .whereColumn(CN_KEY).isEqualTo(literal(choosingKey))
                        .whereColumn(CN_INSTANCEID).isEqualTo(literal(instance.getInstanceId()))
                        .build()
        );
    }

    public void deleteInstanceEntry(AppsInstance instance) throws Exception {
        // Acquire the lock first
        getLock(instance);

        // Delete the row
        final String key = findKey(instance.getApp(), String.valueOf(instance.getId()), instance.getDatacenter(),
                instance.getRack());
        if (key == null)
            return; // don't fail it
        this.bootSession.execute(
                deleteFrom(CF_NAME_TOKENS)
                        .whereColumn(CN_KEY).isEqualTo(literal(key))
                        .build()
        );

        final String lockKey = getLockingKey(instance);
        this.bootSession.execute(
                deleteFrom(CF_NAME_LOCKS)
                        .whereColumn(CN_KEY).isEqualTo(literal(lockKey))
                        .whereColumn(CN_INSTANCEID).isEqualTo(literal(instance.getInstanceId()))
                        .build()
        );

        final String choosingKey = getChoosingKey(instance);
        this.bootSession.execute(
                deleteFrom(CF_NAME_LOCKS)
                        .whereColumn(CN_KEY).isEqualTo(literal(choosingKey))
                        .whereColumn(CN_INSTANCEID).isEqualTo(literal(instance.getInstanceId()))
                        .build()
        );
    }

    public AppsInstance getInstance(String app, String rack, int id) {
        Set<AppsInstance> set = getAllInstances(app);
        for (AppsInstance ins : set) {
            if (ins.getId() == id && ins.getRack().equals(rack))
                return ins;
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

    private Set<AppsInstance> getAllInstancesFromCassandra(String app) {
        Set<AppsInstance> set = new HashSet<AppsInstance>();
        try {

            final List<Row> rows = this.bootSession.execute(
                    selectFrom(CF_NAME_TOKENS)
                            .all()
                            .whereColumn(CN_APPID).isEqualTo(literal(app))
                            .build()
            ).all();
            for (final Row row : rows) {
                set.add(transform(row));
            }
        } catch (Exception e) {
            logger.warn("Caught an Unknown Exception during reading msgs ... -> " + e.getMessage());
            throw new RuntimeException(e);
        }
        return set;
    }

    public Set<AppsInstance> getAllInstances(String app) {
        if (isCassandraCacheExpired() || appInstances.isEmpty()) {
            write.lock();
            if (isCassandraCacheExpired() || appInstances.isEmpty()) {
                logger.debug("lastpull %d msecs ago, getting instances from C*", System.currentTimeMillis() - lastTimeCassandraPull);
                appInstances = getAllInstancesFromCassandra(app);
                lastTimeCassandraPull = System.currentTimeMillis();
            }
            write.unlock();
        }
        read.lock();
        Set<AppsInstance> retInstances = appInstances;
        read.unlock();
        return retInstances;
    }

    private String findKey(String app, String id, String location, String datacenter) {
        try {
            final Row row = this.bootSession.execute(
                    selectFrom(CF_NAME_TOKENS)
                            .all()
                            .whereColumn(CN_APPID).isEqualTo(literal(app))
                            .whereColumn(CN_ID).isEqualTo(literal(id))
                            .whereColumn(CN_LOCATION).isEqualTo(literal(location))
                            .whereColumn(CN_DC).isEqualTo(literal(datacenter))
                            .build()
            ).one();
            if (row == null) {
                return null;
            }

            return row.getString(CN_KEY);
        } catch (Exception e) {
            logger.warn("Caught an Unknown Exception during find a row matching cluster[" + app + "], id[" + id
                    + "], and region[" + datacenter + "]  ... -> " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private AppsInstance transform(final Row row) {
        final AppsInstance ins = new AppsInstance();
        ins.setApp(row.getString(CN_APPID));
        ins.setZone(row.getString(CN_AZ));
        ins.setHost(row.getString(CN_HOSTNAME));
        ins.setDynomitePort(!row.isNull(CN_DYNOMITE_PORT) ? row.getInt(CN_DYNOMITE_PORT) : commonConfig.getDynomitePort());
        ins.setDynomiteSecurePort(!row.isNull(CN_DYNOMITE_SECURE_PORT) ? row.getInt(CN_DYNOMITE_SECURE_PORT) : commonConfig.getDynomiteSecurePort());
        ins.setDynomiteSecureStoragePort(!row.isNull(CN_DYNOMITE_SECURE_STORAGE_PORT) ? row.getInt(CN_DYNOMITE_SECURE_STORAGE_PORT) : commonConfig.getDynomiteSecureStoragePort());
        ins.setPeerPort(!row.isNull(CN_PEER_PORT) ? row.getInt(CN_PEER_PORT) : commonConfig.getDynomitePeerPort());
        ins.setHostIP(row.getString(CN_EIP));
        ins.setId(Integer.parseInt(row.getString(CN_ID)));
        ins.setInstanceId(row.getString(CN_INSTANCEID));
        ins.setDatacenter(row.getString(CN_DC));
        ins.setRack(row.getString(CN_LOCATION));
        ins.setToken(row.getString(CN_TOKEN));
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

    private CqlSession init() {
        final Supplier<List<Host>> supplier;
        if (cassCommonConfig.isEurekaHostsSupplierEnabled()) {
            supplier = this.hostSupplier.getSupplier(BOOT_CLUSTER);
        } else {
            supplier = getSupplier();
        }

        final List<InetSocketAddress> contactPoints = supplier.get().stream().map(host -> {
            final int port = host.getPort() > 0 ? host.getPort() : cassandraPort;
            return new InetSocketAddress(host.getName(), port);
        }).collect(Collectors.toList());

        final String datacenter = !Strings.isNullOrEmpty(cassCommonConfig.getCassandraDatacenterName())
                ? cassCommonConfig.getCassandraDatacenterName() : commonConfig.getRegion();

        return CqlSession.builder()
                .addContactPoints(contactPoints)
                .withLocalDatacenter(datacenter)
                .withKeyspace(KS_NAME)
                .build();
    }

    private Supplier<List<Host>> getSupplier() {

        return new Supplier<List<Host>>() {

            @Override
            public List<Host> get() {

                List<Host> hosts = new ArrayList<Host>();

                List<String> cassHostnames = new ArrayList<String>(
                        Arrays.asList(StringUtils.split(cassCommonConfig.getCassandraSeeds(), ",")));

                if (cassHostnames.size() == 0)
                    throw new RuntimeException(
                            "Cassandra Host Names can not be blank. At least one host is needed. Please use getCassandraSeeds() property.");

                for (String cassHost : cassHostnames) {
                    logger.info("Adding Cassandra Host = {}", cassHost);
                    hosts.add(new Host(cassHost, cassandraPort));
                }

                return hosts;
            }
        };
    }
}
