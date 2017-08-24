package com.netflix.nfsidecar.supplier;

import java.util.List;

import com.google.common.base.Supplier;
import com.netflix.astyanax.connectionpool.Host;

public interface HostSupplier {
        public Supplier<List<Host>> getSupplier(String clusterName);
}