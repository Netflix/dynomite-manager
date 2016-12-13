# Dynomite Manager Configuration
Dynomite-manager leverages [Guice](https://github.com/google/guice) to reduce repetition and in favor of a more readable configuration. The registration of the listener takes place in the [web.xml](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/webapp/WEB-INF/web.xml). [InjectedWebListener](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/InjectedWebListener.java) is the logical place where the injectors are created and configured. 

## Functional Areas
|Configuration |Interface|Description|
|----|-----------|------------|
| Properties | [IConfiguration](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/IConfiguration.java) | system level configuration can be passed. It leverages a file, or [Archaius](https://github.com/Netflix/Archaius). |
| Application | [IAppsInstanceFactory](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/identity/IAppsInstanceFactory.java) |  create, delete, update etc. instance level operations like application name, instance ID, hostname, IP address, Rack, attached Volumes and Tokens. This information can be stored in an external location. |
| Storage | [IStorageProxy](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/storage/IStorageProxy.java) | storage related functionalities like warm up, healthcheck, taking disk snapshot etc. |
| Environment | [InstanceDataRetriever](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/config/InstanceDataRetriever.java) |  receive information about Rac, public hostname, public IP, Instance ID and type. Mainly these are reported by the environment. |
| Fast Properties | [IConfigSource](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/IConfigSource.java) | configuration source, internally at Netflix we use [Archaius](https://github.com/Netflix/Archaius). |

## Default Implementation

    binder().bind(IConfiguration.class).to(DynomitemanagerConfiguration.class);
    binder().bind(ProcessTuner.class).to(FloridaStandardTuner.class);
    binder().bind(IAppsInstanceFactory.class).to(CassandraInstanceFactory.class);
    binder().bind(SchedulerFactory.class).to(StdSchedulerFactory.class).asEagerSingleton();
    binder().bind(ICredential.class).to(IAMCredential.class);
    binder().bind(IFloridaProcess.class).to(FloridaProcessManager.class);
    binder().bind(IStorageProxy.class).to(RedisStorageProxy.class);
    binder().bind(InstanceDataRetriever.class).to(AwsInstanceDataRetriever.class);


However, one can implement the interfaces based on their deployment and environment. The following implementations are provided:

* [DynomitemanagerConfiguration](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/DynomitemanagerConfiguration.java) contains the default configuration properties. Properties inherently use [Archaius](https://github.com/Netflix/Archaius) configuration.
* [FloridaStandardTuner](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/FloridaStandardTuner.java) contains the configuration to be written in the dynomite.yml file. More information about the yml file can be found in the [Dynomite repo](https://github.com/Netflix/dynomite/tree/dev/conf).
* [CassandraInstanceFactory](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/identity/CassandraInstanceFactory.java) provides an implementation in Cassandra for token management. The configuration of the Cassandra cluster is defined in the [DynomitemanagerConfiguration](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/DynomitemanagerConfiguration.java#L87-L90)
* StdSchedulerFactory is the standard Quartz scheduler implementation.
* [IAMCredential](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/aws/IAMCredential.java) credentials provided by the AWS and instance credentials provider.
* [FloridaProcessManager](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/FloridaStandardTuner.java) is the administrative process for Dynomite (start, stop, write-only etc.)
* [RedisStorageProxy](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/storage/RedisStorageProxy.java) handles the Redis storage level. The implementations for Redis is complete, further storage deployments can be added in [storage package](https://github.com/Netflix/dynomite-manager/tree/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/storage)
* [AwsInstanceDataRetriever](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/config/AwsInstanceDataRetriever.java) provides information about the instance from AWS. Implementations for VPC and local deployments are provided in the [config package](https://github.com/Netflix/dynomite-manager/tree/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/config). For running Dynomite Manager locally in your system, you can use [LocalInstanceDataRetriever](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/config/LocalInstanceDataRetriever.java).
* [SimpleDBConfigSource](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/SimpleDBConfigSource.java) provides an interface to store and retrieve configuration information using SimpleDB. Further implementations on how to use 

## Host Supplier
One can provide its own host supplier. In this case the `config.isEurekaHostSupplierEnabled()` must return [false](https://github.com/Netflix/dynomite-manager/blob/35e2b2c4c4e19b92ccf3f50d013e61185ffc0fdb/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/DynomitemanagerConfiguration.java#L636-L639).

There are two exemplar host suppliers in the corresponding [package](https://github.com/Netflix/dynomite-manager/tree/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/supplier). One based on Eureka and one based on a locally provided host supplier.

### How to Use Configuration source:

Properties inherently use [Archaius](https://github.com/Netflix/Archaius) configuration.

But you can use any of the above methods to supply properties whichever way you would like. (e.g. [Dynomitemanager.properties](https://github.com/Netflix/dynomite-manager/blob/dev/conf/dynomitemanager.properties) or System properties)

_**Another way**_ to provide your properties is by using dynomitemanager{version}.jar in your web container and 
then implementing IConfiguration interface.