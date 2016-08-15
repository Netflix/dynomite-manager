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
package com.netflix.dynomitemanager.sidecore.storage;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.utils.JedisUtils;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.LOCAL_ADDRESS;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.REDIS_PORT;


//TODOs: we should talk to admin port (22222) instead of 8102 for both local and peer
@Singleton
public class RedisStorageProxy implements IStorageProxy {
  
	private static final Logger logger = LoggerFactory.getLogger(RedisStorageProxy.class);

  private Jedis localJedis;


  @Inject
  private IConfiguration config; 

  @Inject
  private Sleeper sleeper;

  @Inject
  private InstanceState instanceState;

  public RedisStorageProxy() {
      //connect();
  }
  
  private void connect() {
      try {
          if (localJedis == null)
             localJedis = new Jedis(LOCAL_ADDRESS, REDIS_PORT, 5000);
          else localJedis.disconnect();
          
          localJedis.connect();
      } catch (Exception e) {
          logger.info("Unable to connect: " + e.getMessage());
      }
  }
  
  
/*    private boolean isAlive(Jedis jedis) {
      try {
         jedis.ping();
      } catch (JedisConnectionException e) {
          connect();
          return false; 
      } catch (Exception e) {
          connect();
          return false;
      }
      return true;
  }*/
  
  //issue a 'slaveof peer port to local redis
  private void startPeerSync(String peer, int port) { 
      boolean isDone = false;
      connect();
      
      while (!isDone) {
         try {
             //only sync from one peer for now
             isDone = (localJedis.slaveof(peer, port) != null);
             sleeper.sleepQuietly(1000);
         } catch (JedisConnectionException e) {
             connect();
         } catch (Exception e) {
             connect();
         }
      }
  }
  
  //issue a 'slaveof no one' to local reids
  //set dynomite to accept writes but no reads
  @Override
  public void stopPeerSync() {
      boolean isDone = false;
      
      while (!isDone) {
          logger.info("calling slaveof no one");
          try {
             isDone = (localJedis.slaveofNoOne() != null);
             sleeper.sleepQuietly(1000);
             
          } catch (JedisConnectionException e) {
             logger.error("JedisConnection Exception in Slave of None: " + e.getMessage());
             connect();
          } catch (Exception e) {
             logger.error("Error: " + e.getMessage()); 
             connect();
          }
      }
  }

  @Override
  public boolean takeSnapshot()
  {           
      connect();
      try {
      	if(config.isAof()) {
             logger.info("starting Redis BGREWRITEAOF");
      	   localJedis.bgrewriteaof();
      	}
      	else {
             logger.info("starting Redis BGSAVE");
      	   localJedis.bgsave();

      	}
      	/* We want to check if a bgrewriteaof was already scheduled
      	 * or it has started. If a bgrewriteaof was already scheduled
      	 * then we should get an error from Redis but should continue.
      	 * If a bgrewriteaof has started, we should also continue.
      	 * Otherwise we may be having old data in the disk.
      	 */
      } catch (JedisDataException e) {
      	String scheduled = null;
      	if (!config.isAof()) {
      		scheduled = "ERR Background save already in progress";
      	}
      	else {
              scheduled = "ERR Background append only file rewriting already in progress";
      	}
      	
          if(!e.getMessage().equals(scheduled)){
              throw e;
          }
      	logger.warn("Redis: There is already a pending BGREWRITEAOF/BGSAVE.");
      }

      String peerRedisInfo = null;
      int retry = 0;
      
      try {
          while(true) {
          	peerRedisInfo = localJedis.info();
              Iterable<String> result = Splitter.on('\n').split(peerRedisInfo); 
              String pendingPersistence = null;
              
              for(String line : result) {
                  if ((line.startsWith("aof_rewrite_in_progress") && config.isAof())  || 
                  		(line.startsWith("rdb_bgsave_in_progress") && !config.isAof())) {
                      String[] items = line.split(":");
                      pendingPersistence = items[1].trim();
                      if(pendingPersistence.equals("0")){
                      	logger.info("Redis: BGREWRITEAOF/BGSAVE completed.");
                      	return true;
                      } else {
                      	retry++;
                      	logger.warn("Redis: BGREWRITEAOF/BGSAVE pending. Sleeping 30 secs...");
                          sleeper.sleepQuietly(30000);

                          if (retry > 20) {
                          	return false;
                          }
                      }
                  }
              }
          }
          
      } catch (JedisConnectionException e) {
      	logger.error("Cannot connect to Redis to perform BGREWRITEAOF/BGSAVE");
      }
     
      logger.error("Redis BGREWRITEAOF/BGSAVE was not succesful.");
	   return false;

  }


  @Override
  public boolean loadingData()
  {
  	connect();
      logger.info("loading AOF from the drive");
      String peerRedisInfo = null;
      int retry = 0;
      
      try {
      	peerRedisInfo = localJedis.info();
          Iterable<String> result = Splitter.on('\n').split(peerRedisInfo); 
          String pendingAOF = null;
          
          for(String line : result) {
              if (line.startsWith("loading")) {
              	 String[] items = line.split(":");
                   pendingAOF = items[1].trim();
                   if(pendingAOF.equals("0")){
                   	logger.info("Redis: memory loading completed.");
                   	return true;
                   } else {
                   	retry++;
                   	logger.warn("Redis: memory pending. Sleeping 30 secs...");
                       sleeper.sleepQuietly(30000);

                       if (retry > 20) {
                       	return false;
                       }
                   }
               }
           }
      } catch (JedisConnectionException e) {
  	   logger.error("Cannot connect to Redis to load the AOF");
      }
      
	   return false;

  }


  @Override
  public boolean isAlive() {
      // Not using localJedis variable as it can be used by
      // ProcessMonitorTask as well.
      return JedisUtils.isAliveWithRetry(DynomitemanagerConfiguration.LOCAL_ADDRESS, REDIS_PORT);
  }

  @Override
  public long getUptime() {
      
      return 0;
  }

  @Override
  //probably use our Retries Util here
  public Bootstrap warmUpStorage(String[] peers) {
      String alivePeer = null;
      Jedis peerJedis = null;
      
      // Identify if we can connect with the peer node
      for(String peer : peers) {
          logger.info("Peer node [" + peer + "] has the same token!" );
          peerJedis = JedisUtils.connect(peer, config.getListenerPort());
          if (peerJedis != null && isAlive()) {
              alivePeer = peer;
              break;
          } 
      }
      
      // We check if the select peer is alive and we connect to it.
      if (alivePeer == null) {
      	logger.error("Cannot connect to peer node to bootstrap");
      	return Bootstrap.CANNOT_CONNECT_FAIL;
      }
      else {   
          logger.info("Issue slaveof command on peer[" + alivePeer + "] and port[" + REDIS_PORT + "]");
          startPeerSync(alivePeer, REDIS_PORT);
          

          long diff = 0;
          long previousDiff = 0;
          short retry = 0;
          short numErrors = 0;
          long startTime = System.currentTimeMillis();
          
          /*
           * Conditions under which warmp up will end
           * 1. number of Jedis errors are 5.
           * 2. number of consecutive increases of offset differences (caused when client produces high load).
           * 3. the difference between offsets is very small or zero (success).
           * 4. warmp up takes more than FP defined minutes (default 20 min).
           * 5. Dynomite has started and is healthy.
           */
          
          while (numErrors < 5) {
          	// sleep 10 seconds in between checks
              sleeper.sleepQuietly(10000);
              try {
                  diff = canPeerSyncStop(peerJedis, startTime);
              } catch (Exception e) {
              	numErrors++;
              }
              
              /*
               * Diff meaning:
               * a. diff ==  0 --> we are either in sync or close to sync.
               * b. diff == -1 --> there was an error in sync process.
               * c. diff == -2 --> offset is still zero, peer syncing has not started.
               * d. diff == -3 --> warm up lasted more than bootstrapTime
               */
              if (diff == 0) {
              	break;
              }
              else if (diff == -1) {
              	logger.error("There was an error in the warm up process - do NOT start Dynomite");  
                  peerJedis.disconnect();
                  return Bootstrap.WARMUP_ERROR_FAIL;
              }                
              else if (diff == -2 ) {
              	startTime = System.currentTimeMillis();
              }
              else if (diff == -3 ) {
                  peerJedis.disconnect();
              	return Bootstrap.EXPIRED_BOOTSTRAPTIME_FAIL;
              }
           
              
              /*
               * Exit conditions:
               * a. retry more than 5 times continuously and if the diff is larger than the previous diff.
               */
              if (previousDiff < diff) {
              	logger.info("Previous diff (" + previousDiff +") was smaller than current diff (" + diff  +") ---> Retry effort: " + retry);
              	retry++;
              	if (retry == 10){
                  	logger.error("Reached 10 consecutive retries, peer syncing cannot complete");
                      peerJedis.disconnect();
                      return Bootstrap.RETRIES_FAIL;                   	
              	}
              }
              else{
              	retry = 0;
              }
              previousDiff = diff;
          }
          
          peerJedis.disconnect();

          if (diff > 0) {
              logger.info("Stopping peer syncing with difference: " + diff);
          }
      }

      return Bootstrap.IN_SYNC_SUCCESS;
  }
  
  /**
   * Resets Redis to master if it was slave due to warm up failure.
   */
  @Override
  public boolean resetStorage() {
  	logger.info("Checking if Redis needs to be resetted to master");
      connect();
      String peerRedisInfo = null;
      try {
      	peerRedisInfo = localJedis.info();
      } catch (JedisConnectionException e) {
      	// Try to reconnect
      	try {
      		connect();
          	peerRedisInfo = localJedis.info();
      	} catch (JedisConnectionException ex) {
      		logger.error("Cannot connect to Redis");
      		return false;
      	}
      }
      Iterable<String> result = Splitter.on('\n').split(peerRedisInfo);
      
      String role = null;

      for(String line : result) {
          if (line.startsWith("role")) {
              String[] items = line.split(":");
           //   logger.info(items[0] + ": " + items[1]);
              role = items[1].trim();
              if(role.equals("slave")){
              	logger.info("Redis: slave ----> master");
              	stopPeerSync();
              }
              return true;
          }
      }
      
      return false;

  }


  private Long canPeerSyncStop(Jedis peerJedis, long startTime) throws RedisSyncException {
  	
  	if (System.currentTimeMillis() - startTime > config.getMaxTimeToBootstrap()) {
         	logger.warn("Warm up takes more than " + config.getMaxTimeToBootstrap()/60000 + " minutes --> moving on");
         	return (long) -3;
      }
  	
      logger.info("Checking for peer syncing");
      String peerRedisInfo = peerJedis.info();
      
      Long masterOffset = -1L;
      Long slaveOffset = -1L;
      
      //get peer's repl offset
      Iterable<String> result = Splitter.on('\n').split(peerRedisInfo);
      
      for(String line : result) {
          if (line.startsWith("master_repl_offset")) {
              String[] items = line.split(":");
              logger.info(items[0] + ": " + items[1]);
              masterOffset = Long.parseLong(items[1].trim());
              
          }
          
          //slave0:ip=10.99.160.121,port=22122,state=online,offset=17279,lag=0
          if (line.startsWith("slave0")) {
              String[] items = line.split(",");
              for(String item : items) {
                  if (item.startsWith("offset")) {
                      String[] offset = item.split("=");
                      logger.info(offset[0] + ": " + offset[1]);
                      slaveOffset = Long.parseLong(offset[1].trim());
                  }           
              }
          }
      }

      if (slaveOffset == -1) {
      	logger.error("Slave offset could not be parsed --> check memory overcommit configuration");
      	return (long) -1;
      }
      else if (slaveOffset == 0) {
      	logger.info("Slave offset is zero ---> Redis master node still dumps data to the disk");
      	return (long) -2;
      }
      Long diff = Math.abs(masterOffset - slaveOffset);
      
      logger.info("masterOffset: " + masterOffset + " slaveOffset: " + slaveOffset + 
      		" current Diff: " + diff + 
      		" allowable diff: " + config.getAllowableBytesSyncDiff());
      /* 
       * Allowable bytes sync diff can be configured by a Fast Property.
       * If the difference is very small, then we return zero.
       */
      if (diff < config.getAllowableBytesSyncDiff()) {
          logger.info("master and slave are in sync!");
          return (long) 0;
      }
      else if (slaveOffset == 0) {
      	logger.info("slave has not started syncing");
      }
      return diff;
  }
  
  private class RedisSyncException extends Exception {
       public RedisSyncException(String msg) {
           super(msg);
       }
  }
  
}

