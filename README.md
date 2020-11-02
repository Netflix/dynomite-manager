# Dynomite-manager

[![Build Status](https://travis-ci.com/Netflix/dynomite-manager.svg)](https://travis-ci.com/Netflix/dynomite-manager)
[![Dev chat at https://gitter.im/Netflix/dynomite](https://badges.gitter.im/Netflix/dynomite.svg)](https://gitter.im/Netflix/dynomite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Apache V2 License](http://img.shields.io/badge/license-Apache%20V2-blue.svg)](https://github.com/Netflix/dynomite-manager/blob/dev/LICENSE)


**Dynomite-manager** is a Java process/tool that can be used alongside [Dynomite](https://github.com/Netflix/dynomite) to manage a Dynomite cluster. Some of the features include:
- [Discovery and Instance Healthcheck](https://github.com/Netflix/dynomite-manager/wiki/Discovery-and-Healthcheck)
- Node Configuration and Token Management for multi-region deployments
- Dynamic,Typed Properties based on [Archaius 2](https://github.com/Netflix/archaius)
-- High throughput and Thread Safe Configuration operations.
- [Dynomite/Redis cold bootstrap (warm-up)](https://github.com/Netflix/dynomite-manager/wiki/Cold-Bootstraping)
- [Monitoring and Insights Integration](https://github.com/Netflix/dynomite-manager/wiki/Monitoring-and-Insights-Integration)
- Support multi-region Dynomite deployment via public IP.
- Automated security group update in a multi-region environment.
- [Object storage backups (AWS S3 implementation provided)](https://github.com/Netflix/dynomite-manager/wiki/S3-Backups-and-Restores)
- [REST API](https://github.com/Netflix/dynomite-manager/wiki/REST-API)

Details about the features can be found in the [Wiki](https://github.com/Netflix/dynomite-manager/wiki)

## Workflow

The stable version of Dynomite-manager is the [master]( https://github.com/Netflix/dynomite-manager/tree/master ) branch. 

For questions or contributions, please consider reading [CONTRIBUTING.md](CONTRIBUTING.md).

## Build

Dynomite-manager comes with a Gradle wrapper

    ./gradlew build

The gradlew script will pull down all necessary gradle components/infrastructure automatically, then run the build.

Dynomite-manager provides several default implementations (AWS, Configuration, credentials etc). You can use these or choose to create your own. Dynomite-manager is currently working on AWS and your local environment. We are open to contributions to support other platforms as well. 

## Howto

At Netflix, we use AWS to deploy Dynomite-manager in an EC2 instance along with Dynomite and Redis. However, Dynomite-manager has a set of abstracted interfaces that it can work in different environments. The following are the [AWS bindings](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/java/com/netflix/florida/startup/FloridaModule.java#L101-L108). In a different environment, these interfaces must be implemented. By default, we provide a [local binding](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/java/com/netflix/florida/startup/FloridaModule.java#L111) for getting the instance information from a local-end point.

Secondly, we use Dynomite-manager as a sidecar for the communication of Dynomite with the highly avalaible database where the tokens are stored. For that reason, we use Cassandra. The [default instance factory](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/java/com/netflix/florida/startup/FloridaModule.java#L115) binding is about Cassandra but any other data store could be used. Moreover, in order to get the information about the Cassandra nodes, we provide a [Local Host supplier](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/java/com/netflix/florida/startup/FloridaModule.java#L116) as well as implementations for [Eureka discovery service](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-web/src/main/java/com/netflix/florida/startup/FloridaModule.java#L118). 

### Deploying in a Tomcat Container

1. Build the dynomite-manager
2. Set up Auto-Scale Group (ASG) and spin up instances
3. Install [Dynomite](https://github.com/Netflix/dynomite) and web container (such as tomcat) on the instances.
4. Setup AWS credentials
5. Deploy `dynomite-manager.war` in your container

### Integrating with other systems

Dynomite-manager also publishes the corresponding .jars that one can use to integrate with their own systems. At Netflix, we do use this approach. We bring the OSS .jars in our build process that produces a .war file. 

## Configuration

Dynomite-manager is comprised of 4 configuration interfaces.

* [FloridaConfig](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-core/src/main/java/com/netflix/dynomitemanager/config/FloridaConfig.java): Configuration for everything related to Dynomite and Redis.
* [AwsCommonConfig](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-common/src/main/java/com/netflix/nfsidecar/config/AWSCommonConfig.java): AWS configuration. If DM is deployed in another environment, then the corresponding interface should be added.
* [CassCommonConfig](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-common/src/main/java/com/netflix/nfsidecar/config/CassCommonConfig.java): Cassandra configuration. This is the configuration for Cassandra which stores the Dynomite tokens. If another DB is used, then the corresponding interface should be added.
* [CommonConfig](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager-common/src/main/java/com/netflix/nfsidecar/config/CommonConfig.java): Baseline configuration.

## Help

We are looking forward in your contributions. If you need some help with either getting up and going or some problems with the code?

   * Submit an issue to repo
   * Chat with us on [![Dev chat at https://gitter.im/Netflix/dynomite](https://badges.gitter.im/Netflix/dynomite.svg)](https://gitter.im/Netflix/dynomite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
