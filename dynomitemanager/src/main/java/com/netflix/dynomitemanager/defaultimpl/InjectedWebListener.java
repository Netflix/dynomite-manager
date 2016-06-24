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

package com.netflix.dynomitemanager.defaultimpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.netflix.dynomitemanager.FloridaServer;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.identity.CassandraInstanceFactory;
import com.netflix.dynomitemanager.identity.DefaultVpcInstanceEnvIdentity;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.aws.IAMCredential;
import com.netflix.dynomitemanager.sidecore.config.AwsInstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.config.LocalInstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.scheduler.GuiceJobFactory;
import com.netflix.dynomitemanager.defaultimpl.FloridaStandardTuner;
import com.netflix.dynomitemanager.sidecore.utils.ProcessTuner;
import com.netflix.dynomitemanager.IFloridaProcess;
import com.netflix.dynomitemanager.defaultimpl.FloridaProcessManager;
import com.netflix.dynomitemanager.identity.AwsInstanceEnvIdentity;
import com.netflix.dynomitemanager.identity.CassandraInstanceFactory;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;
import com.netflix.dynomitemanager.identity.LocalInstanceEnvIdentity;
import com.netflix.dynomitemanager.sidecore.storage.RedisStorageProxy;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.storage.RedisStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.FloridaHealthCheckHandler;
import com.netflix.dynomitemanager.sidecore.utils.ProcessTuner;
import com.netflix.dynomitemanager.supplier.HostSupplier;
import com.netflix.dynomitemanager.supplier.LocalHostsSupplier;
import com.netflix.karyon.spi.HealthCheckHandler;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 *
 */
public class InjectedWebListener extends GuiceServletContextListener {

    protected static final Logger logger = LoggerFactory.getLogger(InjectedWebListener.class);

    @Override
    protected Injector getInjector() {
        List<Module> moduleList = Lists.newArrayList();
        moduleList.add(new JaxServletModule());
        moduleList.add(new DynomiteGuiceModule());
        Injector injector = Guice.createInjector(moduleList);
        // Initialize DynomiteManager configuration
        try
        {       	
            injector.getInstance(IConfiguration.class).initialize();
        }
        catch (Exception e)
        {
                logger.error("IConfiguration: " + e.getMessage(),e);
                throw new RuntimeException(e.getMessage(), e);
        }
        
        // Initialize Florida Server
        try
        {       
            injector.getInstance(FloridaServer.class).initialize();
        }
        catch (Exception e)
        {
            logger.error("Florida Server: " + e.getMessage(),e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return injector;
    }

    public static class JaxServletModule extends ServletModule
    {
        @Override
        protected void configureServlets()
        {

            Map<String, String> params = new HashMap<String, String>();
            params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "unbound");
            params.put("com.sun.jersey.config.property.packages", "com.netflix.dynomitemanager.resources");
            serve("/REST/*").with(GuiceContainer.class, params);
        }
    }


    public static class DynomiteGuiceModule extends AbstractModule
    {
        @Override
        protected void configure()
        {
            logger.info("**Binding OSS Config classes.");
            binder().bind(IConfiguration.class).to(DynomitemanagerConfiguration.class);
            binder().bind(ProcessTuner.class).to(FloridaStandardTuner.class);
            binder().bind(IAppsInstanceFactory.class).to(CassandraInstanceFactory.class);
            binder().bind(SchedulerFactory.class).to(StdSchedulerFactory.class).asEagerSingleton();
            binder().bind(ICredential.class).to(IAMCredential.class);
            binder().bind(IFloridaProcess.class).to(FloridaProcessManager.class);
            binder().bind(IStorageProxy.class).to(RedisStorageProxy.class);
            binder().bind(InstanceDataRetriever.class).to(AwsInstanceDataRetriever.class);
            //binder().bind(HostSupplier.class).to(EurekaHostsSupplier.class);
            binder().bind(HostSupplier.class).to(LocalHostsSupplier.class);
            binder().bind(InstanceEnvIdentity.class).to(DefaultVpcInstanceEnvIdentity.class).asEagerSingleton();
            binder().bind(HealthCheckHandler.class).to(FloridaHealthCheckHandler.class).asEagerSingleton();
            //binder().bind(InstanceDataRetriever.class).to(LocalInstanceDataRetriever.class);
            //binder().bind(HostSupplier.class).to(LocalHostsSupplier.class);
            //binder().bind(InstanceEnvIdentity.class).to(LocalInstanceEnvIdentity.class);

 //           binder().bind(GuiceContainer.class).asEagerSingleton();
 //           binder().bind(GuiceJobFactory.class).asEagerSingleton();

        }
    }

}
