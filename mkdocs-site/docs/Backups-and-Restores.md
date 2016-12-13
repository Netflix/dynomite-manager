Dynomite can be used as a single point of truth (data store) as well as a cache. A dependable backup and recovery process is therefore critical for Disaster Recovery (DR) and Data Corruption (CR) when choosing a data store in the cloud. With Dynomite-manager, a daily snapshot for all clusters that leverage Dynomite as a data store is used to back them up to an object storage. The default implementation is currently S3; S3 was an obvious choice due to its simple interface and ability to access any amount of data from anywhere.

## How it works
**Backup**:
Dynomite-manager initiates the S3 backups. The backups feature leverages the persistence feature of Redis to dump data to the drive. Dynomite-manager supports both the RDB and the AOF persistence of Redis, offering the ability to the users to use a readable format of their data for debugging or a memory direct snapshot. The backups leverage the IAM credentials in order to encrypt the communication. Backups (a) can be scheduled using a date in the configuration (or by leveraging Archaius, Netflix configuration management API), and (b) on demand using the REST API. 

**Restores**:
Dynomite-manager supports restoring a single node through a REST API, or the complete ring. When performing a restore, Dynomite-manager (on each node), shuts down Redis and Dynomite, locates the snapshot files in S3, and orchestrates the download of the files. Once the snapshot is transferred to the node, Dynomite-manager starts Redis and waits until the data are in memory, and then follows up with starting the Dynomite process. Dynomite-manager can also restore data to clusters with different names. This allows us to spin up multiple test clusters with the same data, enabling refreshes. Refreshes are very important at Netflix, because cluster users can leverage production data in a test environment, hence perform realistics benchmarks and offline analysis on production data. Finally, Dynomite-manager allows for targeted refreshes on a specific date, allowing cluster users to restore data to point prior to the data corruption, test production data for a specific time frame and opening the doors for many other use cases that we have not yet explored.

## Setting up backups/restores
Dynomite-manager uses the fast properties, or external configuration, to define the backup/restore buckets as well as other related properties

* `dynomitemanager.dyno.backup.bucket.name`: Object storage bucket name, where backed up files will be stored
* `dynomitemanager.dyno.backup.restore.enabled`: `false` by default, enabling restores
* `dynomitemanager.dyno.backup.snapshot.enabled`: `false` by default, enabling backups