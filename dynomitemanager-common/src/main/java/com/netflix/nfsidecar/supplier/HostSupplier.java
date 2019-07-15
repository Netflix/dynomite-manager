package com.netflix.nfsidecar.supplier;

import com.google.common.base.Supplier;

import java.util.List;

public interface HostSupplier {
        public Supplier<List<Host>> getSupplier(String clusterName);
}