/**
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.florida.startup;

import com.google.inject.AbstractModule;
// Common module dependencies
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.dynomitemanager.FloridaServer;
import com.netflix.dynomitemanager.aws.S3Backup;
import com.netflix.dynomitemanager.aws.S3Restore;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.config.InstanceState;
import com.netflix.dynomitemanager.dualAccount.AwsRoleAssumptionCredential;
import com.netflix.dynomitemanager.dynomite.DynomiteProcessManager;
import com.netflix.dynomitemanager.dynomite.DynomiteStandardTuner;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.monitoring.JedisFactory;
import com.netflix.dynomitemanager.monitoring.SimpleJedisFactory;
import com.netflix.dynomitemanager.storage.RedisStorageProxy;
import com.netflix.dynomitemanager.storage.StorageProxy;

import javax.inject.Singleton;

import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.netflix.nfsidecar.aws.AWSMembership;
import com.netflix.nfsidecar.aws.AwsInstanceEnvIdentity;
import com.netflix.nfsidecar.aws.IAMCredential;
import com.netflix.nfsidecar.aws.ICredential;
import com.netflix.nfsidecar.backup.Backup;
import com.netflix.nfsidecar.backup.Restore;
import com.netflix.nfsidecar.config.AWSCommonConfig;
import com.netflix.nfsidecar.config.CassCommonConfig;
import com.netflix.nfsidecar.config.CommonConfig;
import com.netflix.nfsidecar.identity.IInstanceState;
import com.netflix.nfsidecar.identity.IMembership;
import com.netflix.nfsidecar.identity.InstanceEnvIdentity;
import com.netflix.nfsidecar.instance.InstanceDataRetriever;
import com.netflix.nfsidecar.instance.VpcInstanceDataRetriever;
import com.netflix.nfsidecar.resources.env.IEnvVariables;
import com.netflix.nfsidecar.resources.env.InstanceEnvVariables;
import com.netflix.nfsidecar.supplier.HostSupplier;
import com.netflix.nfsidecar.supplier.LocalHostSupplier;
import com.netflix.nfsidecar.tokensdb.CassandraInstanceFactory;
import com.netflix.nfsidecar.tokensdb.IAppsInstanceFactory;
import com.netflix.nfsidecar.utils.ProcessTuner;
import com.netflix.runtime.health.guice.HealthModule;

/**
 * This is the "main" module where we wire everything up. If you see this module
 * getting overly complex, it's a good idea to break things off into separate
 * ones and install them here instead.
 *
 */

public final class FloridaModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new HealthModule() {
            @Override
            protected void configureHealth() {
                bindAdditionalHealthIndicator().to(DynomiteProcessManager.class);
                bindAdditionalHealthIndicator().to(RedisStorageProxy.class);

            }
        });
        install(new JerseyModule());
        install(new ArchaiusModule());
/*
        install(new SwaggerServletModule());
        install(new JaxrsSwaggerModule());
        install(new GuiceServletSwaggerModule());
        */

        bind(FloridaServer.class).asEagerSingleton();

        bind(ProcessTuner.class).to(DynomiteStandardTuner.class);
        bind(SchedulerFactory.class).to(StdSchedulerFactory.class).asEagerSingleton();
        bind(IDynomiteProcess.class).to(DynomiteProcessManager.class);
        bind(StorageProxy.class).to(RedisStorageProxy.class);
        bind(IInstanceState.class).to(InstanceState.class);
        bind(JedisFactory.class).to(SimpleJedisFactory.class);
        bind(IEnvVariables.class).to(InstanceEnvVariables.class);

        /* AWS binding */
        bind(InstanceDataRetriever.class).to(VpcInstanceDataRetriever.class);
        bind(IMembership.class).to(AWSMembership.class);
        bind(ICredential.class).to(IAMCredential.class);
        bind(ICredential.class).annotatedWith(Names.named("awsroleassumption")).to(AwsRoleAssumptionCredential.class);
        bind(InstanceEnvIdentity.class).to(AwsInstanceEnvIdentity.class);
        bind(Backup.class).to(S3Backup.class);
        bind(Restore.class).to(S3Restore.class);

        /* Netflix */
        bind(IAppsInstanceFactory.class).to(CassandraInstanceFactory.class);
        bind(HostSupplier.class).to(LocalHostSupplier.class);
        // bind(HostSupplier.class).to(CassandraLocalHostsSupplier.class);
        // bind(HostSupplier.class).to(EurekaHostsSupplier.class);

    }

    @Provides
    @Singleton
    CommonConfig getCommonConfig(ConfigProxyFactory factory) {
        return factory.newProxy(CommonConfig.class);
    }
    
    @Provides
    @Singleton
    CassCommonConfig getCassCommonConfig(ConfigProxyFactory factory) {
        return factory.newProxy(CassCommonConfig.class);
    }
    
    @Provides
    @Singleton
    AWSCommonConfig getAWSCommonConfig(ConfigProxyFactory factory) {
        return factory.newProxy(AWSCommonConfig.class);
    }

    @Provides
    @Singleton
    FloridaConfig getFloridaConfig(ConfigProxyFactory factory) {
        return factory.newProxy(FloridaConfig.class);
    }
}
