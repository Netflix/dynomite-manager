package com.netflix.nfsidecar.supplier;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.AmazonInfo.MetaDataKey;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Use Eureka to provide a list of Cassandra hosts that contain the complete
 * Dynomite topology.
 *
 * EurekaHostsSupplier provides a {@code Supplier<List<Host>>} via the
 * {DiscoveryManager} (i.e. the Eureka client).
 *
 * Note that the class needs the Eureka application name to discover all
 * instances for that application.
 */
@Singleton
public class EurekaHostSupplier implements HostSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(EurekaHostSupplier.class);

    // Eureka client
    private final DiscoveryClient discoveryClient;

    @Inject
    public EurekaHostSupplier(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * Get a list of Cassandra hosts that contain the complete Dynomite
     * topology.
     *
     * @param clusterName
     *            name of the Dynomite cluster
     * @return a Supplier that returns a list of Cassandra hosts
     */
    @Override
    public Supplier<List<Host>> getSupplier(final String clusterName) {
        return new Supplier<List<Host>>() {

            @Override
            public List<Host> get() {

                if (discoveryClient == null) {
                    LOG.error("Eureka DiscoveryClient cannot be null");
                    throw new RuntimeException("EurekaHostsSupplier needs a non-null DiscoveryClient");
                }

                LOG.debug("Fetching instance list for app: " + clusterName);

                Application app = discoveryClient.getApplication(clusterName.toUpperCase());
                List<Host> hosts = new ArrayList<Host>();

                if (app == null) {
                    LOG.warn("Cluster '{}' not found in Eureka", clusterName);
                    return hosts;
                }

                List<InstanceInfo> ins = app.getInstances();

                if (ins == null || ins.isEmpty()) {
                    LOG.warn("Cluster '{}' found in Eureka but has no instances", clusterName);
                    return hosts;
                }

                hosts = Lists
                        .newArrayList(Collections2.transform(Collections2.filter(ins, new Predicate<InstanceInfo>() {
                            @Override
                            public boolean apply(InstanceInfo input) {
                                return input.getStatus() == InstanceInfo.InstanceStatus.UP;
                            }
                        }), new Function<InstanceInfo, Host>() {
                            @Override
                            public Host apply(InstanceInfo info) {
                                String[] parts = StringUtils.split(StringUtils.split(info.getHostName(), ".")[0], '-');

                                Host host = new Host(info.getHostName(), info.getPort())
                                        .addAlternateIpAddress(StringUtils
                                                .join(new String[] { parts[1], parts[2], parts[3], parts[4] }, "."))
                                        .addAlternateIpAddress(info.getIPAddr()).setId(info.getId());

                                try {
                                    if (info.getDataCenterInfo() instanceof AmazonInfo) {
                                        AmazonInfo amazonInfo = (AmazonInfo) info.getDataCenterInfo();
                                        host.setRack(amazonInfo.get(MetaDataKey.availabilityZone));
                                    }
                                } catch (Throwable t) {
                                    LOG.error("Error getting rack for host " + host.getName(), t);
                                }

                                return host;
                            }
                        }));

                LOG.debug("Found hosts in Eureka. Num hosts: " + hosts.size());

                return hosts;
            }
        };
    }

}
