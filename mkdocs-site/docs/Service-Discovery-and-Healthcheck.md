# Healthcheck
Dynomite-manager schedules a Quartz (lightweight thread) every 15 seconds that checks the health of both Dynomite and the underlying storage engine. Since most of our current production deployments leverage Redis, or storage engines based on Redis Serialization Protocol (RESP), the healthcheck involves a three step approach. 

1. Check if Dynomite and Redis are running as Linux processes. 
2. Check if Dynomite can listen to a Redis `PING` and respond to a Redis `PONG`. This step ensures that the neither Dynomite nor Redis are zombie processes and are operational.
3. Check if Dynomite can respond `OK` to a Redis `SETEX` with 1 second TTL. We use `SETEX` because we can expire the key without needed to fire another delete. This step ensures that although Dynomite and Redis are operational, they can still write traffic, which is not the case if the available memory has been exhausted or Redis for some reason runs in slave mode. 

# Discovery
If any of the above checks are not satisfied, Dynomite-manager informs Eureka (Netflix Service registry for resilient mid-tier load balancing and failover) and the node is removed from Discovery. This ensures that the Dyno client can gracefully failover the traffic to another Dynomite node with the same token.
