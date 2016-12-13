# Dynomite-manager Wiki

**Dynomite** is used at Netflix both (a) as a caching layer in front of Cassandra and ElasticSearch, and (b) as a data store layer by itself. The latter is achieved by keeping 9 copies of the data across 3 regions and 3 availability zones (high availability), client failover, as well as by enabling cold bootstrapping (warm up), S3 backups, and other features. Most of these features are enabled through the use of Dynomite-manager (formerly named Florida).

**Dynomite-manager** is a sidecar specifically developed to manage Netflix’s Dynomite clusters and integrate it with the AWS (and Netflix) Ecosystem. It follows similar design principles from more than 6 years of experience of managing Cassandra with Priam, and ElasticSearch clusters with Raigad. Dynomite-manager design is based on Quartz, a rich-featured open source job scheduling library, and Java interfaces such that it can be extensible to other data store engines and cloud deployments (other than Amazon Web Services).

![dynomiteecosystem](https://cloud.githubusercontent.com/assets/4562887/15484005/a0a8d2da-20ec-11e6-9236-5b6edcc5f79d.jpg)