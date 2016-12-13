In designing our warm up feature, we considered the following principles:

* Do not introduce inconsistencies;
* Do not achieve a fully warmed up node, but get as many data as possible.
* Do not create any major issues to another node (i.e. node used as the source of truth)

Based on these principles, a failure of the warm up process should not cause an issue in the cluster, and it should not stop the newly brought up node.

The cold bootstrapping feature is currently designed around Redis. It leverages the master-slave diskless replication of Redis. Dynomite-manager sets the target node that needs to be warmed up as “slave”, and finds another peer with the same token in the local region. That peer is designated as the master, and therefore forces Redis to transfer an rdb file and load it to Redis. Once the warm up is complete then Redis is switched back to serve traffic as a normal master.

Warm up or bootstrapping is triggered by a termination of a node. This is in turn causes a new token to be generated, which initiates the warm up process. The following is the sequence of operations:

1. Searches for the correct token: Warming up node's own token(s) : 1383429731
2. Determines which peer nodes have the same token. A random peer node within the local region is selected to be used for the warm up which avoids cross-region communication issues.
3. Redis issues [SLAVEOF](http://redis.io/commands/SLAVEOF) command to that peer. Effectively the target Redis instance sets itself as a slave of that node. In addition, Dynomite-manager sets Dynomite to be in standby mode so that traffic is not received and the node remains out of discovery.
4. A Dynomite node is fully warmed up if it has received all the data from the remote at the time the warm up process started. To determine if a node is warmed up we use the difference between the Redis master and the Redis slave offset. Both offsets are calculated from the Redis master node that was selected as the source of warm up. This gives us the correct view of how much data the remote Redis master node has streamed and how much data it believes the Redis slave node has received.
5. Once master and slave are in sync, Dynomite is set to allow writes only.
6. Redis is stopped from peer syncing by using  Redis “SLAVEOF NO ONE” command
7. Dynomite is set back to normal state. Process checks health of Dynomite, if there is an issue Dynomite gets restarted.
8. Done!


