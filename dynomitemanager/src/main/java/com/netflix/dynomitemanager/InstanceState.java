/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.dynomitemanager;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Singleton;

/**
 * Contains the state of the health of processed managed by Florida, and
 * maintains the isHealthy flag used for reporting discovery health check.
 */
@Singleton
public class InstanceState {
    private final AtomicBoolean isSideCarProcessAlive = new AtomicBoolean(false);
    private final AtomicBoolean isBootstrapping = new AtomicBoolean(false);
    private final AtomicBoolean isBackup = new AtomicBoolean(false);
    private final AtomicBoolean isRestore = new AtomicBoolean (false);
    private final AtomicBoolean isStorageProxyAlive = new AtomicBoolean(false);
    private final AtomicBoolean isStorageProxyProcessAlive = new AtomicBoolean(false);
    private final AtomicBoolean isStorageAlive = new AtomicBoolean(false);
    // This is true if storage proxy and storage are alive.
    private final AtomicBoolean isHealthy = new AtomicBoolean(false);
    // State of whether the rest endpoints /admin/stop or /admin/start are invoked
    // If its true then ProcessMonitorTask will suspend its process monitoring tasks.
    private final AtomicBoolean isProcessMonitoringSuspended = new AtomicBoolean(false);

    @Override
    public String toString() {
        return "InstanceState{" +
                "isSideCarProcessAlive=" + isSideCarProcessAlive +
                ", isBootstrapping=" + isBootstrapping +
                ", isStorageProxyAlive=" + isStorageProxyAlive +
                ", isStorageProxyProcessAlive=" + isStorageProxyProcessAlive +
                ", isStorageAlive=" + isStorageAlive +
                ", isHealthy=" + isHealthy +
                ", isProcessMonitoringSuspended=" + isProcessMonitoringSuspended +
                '}';
    }

    public boolean isSideCarProcessAlive() {
        return isSideCarProcessAlive.get();
    }

    public void setSideCarProcessAlive(boolean isSideCarProcessAlive) {
        this.isSideCarProcessAlive.set(isSideCarProcessAlive);
    }

    //@Monitor(name="sideCarProcessAlive", type=DataSourceType.GAUGE)
    public int metricIsSideCarProcessAlive() {
        return isSideCarProcessAlive() ? 1 : 0;
    }

    public boolean isBootstrapping() {
        return isBootstrapping.get();
    }
    
    public boolean isBackingup() {
    	return isBackup.get();
    }
    
    public boolean isRestoring() {
    	return isRestore.get();
    }

    public void setBootstrapping(boolean isBootstrapping) {
        this.isBootstrapping.set(isBootstrapping);
    }
    
    public void setBackingup(boolean isBackup) {
    	this.isBackup.set(isBackup);
    }
    
    public void setRestoring(boolean isRestoring) {
    	this.isRestore.set(isRestoring);
    }

    //@Monitor(name="bootstrapping", type=DataSourceType.GAUGE)
    public int metricIsBootstrapping() {
        return isBootstrapping() ? 1 : 0;
    }

    public boolean isStorageProxyAlive() {
        return isStorageProxyAlive.get();
    }

    public void setStorageProxyAlive(boolean isStorageProxyAlive) {
        this.isStorageProxyAlive.set(isStorageProxyAlive);
        setHealthy();
    }

    //@Monitor(name="storageProxyAlive", type=DataSourceType.GAUGE)
    public int metricIsStorageProxyAlive() {
        return isStorageProxyAlive() ? 1 : 0;
    }

    public boolean isStorageProxyProcessAlive() {
        return isStorageProxyProcessAlive.get();
    }

    public void setStorageProxyProcessAlive(boolean isStorageProxyProcessAlive) {
        this.isStorageProxyProcessAlive.set(isStorageProxyProcessAlive);
    }

    //@Monitor(name="storageProxyProcessAlive", type=DataSourceType.GAUGE)
    public int metricIsStorageProxyProcessAlive() {
        return isStorageProxyProcessAlive() ? 1 : 0;
    }

    public boolean isStorageAlive() {
        return isStorageAlive.get();
    }

    public void setStorageAlive(boolean isStorageAlive) {
        this.isStorageAlive.set(isStorageAlive);
        setHealthy();
    }

    //@Monitor(name="storageAlive", type=DataSourceType.GAUGE)
    public int metricIsStorageAlive() {
        return isStorageAlive() ? 1 : 0;
    }

    public boolean isHealthy() {
        return isHealthy.get();
    }

    private void setHealthy() {
        this.isHealthy.set(isStorageProxyAlive() && isStorageAlive());
    }

    //@Monitor(name="healthy", type=DataSourceType.GAUGE)
    public int metricIsHealthy() {
        return isHealthy() ? 1 : 0;
    }

    public boolean getIsProcessMonitoringSuspended() {
        return isProcessMonitoringSuspended.get();
    }

    public void setIsProcessMonitoringSuspended(boolean ipms) {
        this.isProcessMonitoringSuspended.set(ipms);
    }

    //@Monitor(name="processMonitoringSuspended", type=DataSourceType.GAUGE)
    public int metricIsProcessMonitoringSuspended() {
        return getIsProcessMonitoringSuspended() ? 1 : 0;
    }
}