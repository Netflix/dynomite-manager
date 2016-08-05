# Dynomite-manager

[![Build Status](https://travis-ci.org/Netflix/dynomite-manager.svg)](https://travis-ci.org/Netflix/dynomite-manager)
[![Dev chat at https://gitter.im/Netflix/dynomite](https://badges.gitter.im/Netflix/dynomite.svg)](https://gitter.im/Netflix/dynomite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


**Dynomite-manager** is a Java process/tool that can be used alongside [Dynomite](https://github.com/Netflix/dynomite) to manage a Dynomite cluster. Some of the features include:
- [Discovery and Healthcheck](https://github.com/Netflix/dynomite-manager/wiki/Discovery-and-Healthcheck)
- Node Configuration and Token Management for multi-region deployments
- [Dynomite/Redis cold bootstrap (warm up)](https://github.com/Netflix/dynomite-manager/wiki/Cold-Bootstraping)
- [Monitoring and Insights Integration](https://github.com/Netflix/dynomite-manager/wiki/Monitoring-and-Insights-Integration)
- Support multi-region Dynomite deployment via public IP.
- Automated security group update in multi-region environment.
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

The first step before building dynomite-manager is to configure the interfaces related to your environment in the [InjectedWebListener](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/defaultimpl/InjectedWebListener.java). Checkout the [Wiki](https://github.com/Netflix/dynomite-manager/wiki/Configuration) for further explanation on what interfaces to bind based on your environment. 

1. Build the dynomite-manager
2. Set up Auto-Scale Group (ASG) and spin up instances
3. Install [Dynomite](https://github.com/Netflix/dynomite) and web container (such as tomcat) on the instances.
4. Setup AWS credentials
5. Deploy `dynomite-manager.war` in your container

## Run
Dynomite-manager changes the configuration files for Dynomite and data store (Redis) and starts/stops the services. Hence the web containermust have execute rights on the script to modify the dynomite.yml and data store conf file (redis.conf).

## Configuration

You can provide properties by using dynomite-manager{version}.jar in your web container and then implementing [IConfiguration Interface](https://github.com/Netflix/dynomite-manager/blob/master/dynomitemanager/src/main/java/com/netflix/dynomitemanager/sidecore/IConfiguration.java). More details on the how the configuration can be found in the [Wiki](https://github.com/Netflix/dynomite-manager/wiki/Configuration).

## Help

Need some help with either getting up and going or some problems with the code?

   * Submit an issue to repo
   * Chat with us on [![Dev chat at https://gitter.im/Netflix/dynomite](https://badges.gitter.im/Netflix/dynomite.svg)](https://gitter.im/Netflix/dynomite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## License

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
