## Dynomite vs Cassandra tokens
Dynomite and Cassandra share a lot of commonalities on the way they use tokens. The difference between the two is that Dynomite occupies the whole token range on a per rack basis, whereas Cassandra on a per zone basis (when using token static allocation and not virtual nodes). Therefore, tokens can repeat across racks and in the same datacenter. The rest of the token management is very much similar. In that perspective Dynomite-manager shares a lot of code with [Priam](https://github.com/Netflix/Priam), Netflix Cassandra sidecar.

## Token calculation
Dynomite-manager calculates the token of every node by looking at the number of slots (nodes), by which the token range is divided in the rack, and the position of the node. The tokens are then stored in an external data store along with application id, availability zone, datacenter, instance id, hostname, and elastic IP.  Since nodes are by nature volatile in the cloud, if a node gets replaced, Dynomite-manager in the new node queries the data store to find if a token was pre-generated. At Netflix, we leverage a Cassandra cluster to store this information.

## Node Replacement
Since we run Dynomite on the cloud at any point of time a node can die. A new instance is booted in the same auto-scaling group. Dynomite-manager loads along with the Tomcat server. Dynomite-manager queries AWS to receive the list of nodes in the same auto-scaling group, and queries the external Cassandra cluster for the list of nodes along with their tokens. It compares the two and determines, which node has been terminated and marks the token as _dead_. It then self-assigns the _dead_ token, and follows the warm up procedure so that there is no data loss. After the warm up is complete, it starts Dynomite and the storage engine.

During this process, Dyno is informed either through the discovery service that the node is down, or after N (N=10 by default) errors fails over to another node that has the same token in the same datacenter.

## Token Management
An external structured persistent data store system has to be used. This can be done by implementing the interface [IAppsInstanceFactory](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/identity/IAppsInstanceFactory.java).
If you plan to use Cassandra to handle the tokens the following KEYSPACE and TABLE definitions can be used:

    CREATE KEYSPACE dyno_bootstrap WITH replication = {'class': 'NetworkTopologyStrategy', 'eu-west': '3', 'us-east': '3', 'us-west': '3', 'us-west-2': '3'}  AND durable_writes = true;

    CREATE TABLE dyno_bootstrap.tokens (
        key text PRIMARY KEY,
        "Id" text,
        "appId" text,
        "availabilityZone" text,
        datacenter text,
         "elasticIP" text,
        hostname text,
        "instanceId" text,
        location text,
        "token" text,
        updatetime timeuuid
    )

	CREATE TABLE dyno_bootstrap.locks (
	  key blob,
	  column1 text,
	  value blob,
	PRIMARY KEY (key, column1)
	) WITH COMPACT STORAGE
	  AND CLUSTERING ORDER BY (column1 ASC)
	  AND bloom_filter_fp_chance = 0.01
	  AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
	  AND comment = ''
	  AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
	  AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.SnappyCompressor'}
	  AND dclocal_read_repair_chance = 0.0
	  AND default_time_to_live = 0
	  AND gc_grace_seconds = 864000
	  AND max_index_interval = 2048
	  AND memtable_flush_period_in_ms = 0
	  AND min_index_interval = 256
	  AND read_repair_chance = 1.0
	  AND speculative_retry = 'NONE';

The client code can be found in [CassandraInstranceFactory](https://github.com/Netflix/dynomite-manager/blob/dev/dynomitemanager/src/main/java/com/netflix/dynomitemanager/identity/CassandraInstanceFactory.java).

## How can I see the token assigned token on each Dynomite node
### REST API to Dynomite manager

`/get_seeds`: responds with the hostnames and tokens

`/cluster_describe`: responds with a JSON file of the cluster level information

`/status`: returns the status of the processes managed by Dynomite-manager and itself, including the token of the node

### Dynomite's YAML
The YAML contains the tokens. Dynomite-manager re-writes the YAML whenever a new node joins the ring, such that it is always up to date.

