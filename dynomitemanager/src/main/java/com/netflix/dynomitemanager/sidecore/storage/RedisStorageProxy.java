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
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.utils.JedisUtils;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
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
    private final DynamicStringProperty adminUrl = 
            DynamicPropertyFactory.getInstance().getStringProperty("dynomitemanager.metrics.url", "http://localhost:22222");
    //private final HttpClient client = new HttpClient();

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
    private void stopPeerSync() {
        boolean isDone = false;
        
        while (!isDone) {
            logger.info("calling slaveof no one");
            try {
               isDone = (localJedis.slaveofNoOne() != null);
               sleeper.sleepQuietly(1000);
               
            } catch (JedisConnectionException e) {
               logger.error("Error: " + e.getMessage());
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
        String scheduled = "ERR Background append only file rewriting already in progress";
        connect();
        logger.info("starting Redis BGREWRITEAOF");
        try {

        	localJedis.bgrewriteaof();
        	/* We want to check if a bgrewriteaof was already scheduled
        	 * or it has started. If a bgrewriteaof was already scheduled
        	 * then we should get an error from Redis but should continue.
        	 * If a bgrewriteaof has started, we should also continue.
        	 * Otherwise we may be having old data in the disk.
        	 */
        } catch (JedisDataException e) {
            if(!e.getMessage().equals(scheduled)){
                throw e;
            }
        	logger.warn("Redis: There is already a pending BGREWRITEAOF.");
        }

        String peerRedisInfo = null;
        int retry = 0;
        
        try {
            while(true) {
            	peerRedisInfo = localJedis.info();
                Iterable<String> result = Splitter.on('\n').split(peerRedisInfo); 
                String pendingAOF = null;
                
                for(String line : result) {
                    if (line.startsWith("aof_rewrite_in_progress")) {
                        String[] items = line.split(":");
                        pendingAOF = items[1].trim();
                        if(pendingAOF.equals("0")){
                        	logger.info("Redis: BGREWRITEAOF completed.");
                        	return true;
                        } else {
                        	retry++;
                        	logger.warn("Redis: BGREWRITEAOF pending. Sleeping 30 secs...");
                            sleeper.sleepQuietly(30000);

                            if (retry > 20) {
                            	return false;
                            }
                        }
                    }
                }
            }
            
        } catch (JedisConnectionException e) {
        	logger.error("Cannot connect to Redis to perform BGREWRITEAOF");
        }
       
        logger.error("Redis BGREWRITEAOF was not succesful.");
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
        return JedisUtils.isAliveWithRetry(LOCAL_ADDRESS, REDIS_PORT);
    }

    @Override
    public long getUptime() {
        
        return 0;
    }

    @Override
    //probably use our Retries Util here
    public boolean warmUpStorage(String[] peers) {
        String alivePeer = null;
        Jedis peerJedis = null;
        
        for(String peer : peers) {
            logger.info("Peer node [" + peer + "] has the same token!" );
            peerJedis = JedisUtils.connect(peer, config.getListenerPort());
            if (peerJedis != null && isAlive()) {
                alivePeer = peer;
                break;
            } 
        }

        if (alivePeer != null && peerJedis != null) {   
            logger.info("Issue slaveof commnd on peer[" + alivePeer + "] and port[" + REDIS_PORT + "]");
            startPeerSync(alivePeer, REDIS_PORT);
            
            logger.info("Force Dynomite to be in Standby mode!");
            sendCommand("/state/standby");
            
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
             * 4. warmp up takes more than 15 minutes.
             */
            
            while (numErrors < 5) {
                sleeper.sleepQuietly(10000);
                try {
                    diff = canPeerSyncStop(peerJedis, startTime);
                } catch (Exception e) {
                	numErrors++;
                }
                
                /*
                 * Diff meaning:
                 * a. diff ==  0 --> we are either in sync or close to sync.
                 * b. diff == -1 --> that there was an error in sync process.
                 * c. diff == -2 --> offset is still zero, peer syncing has not started.
                 */
                if (diff == 0 || diff == -1 ) {
                	break;
                }
                else if (diff == -2 ) {
                	startTime = System.currentTimeMillis();
                }

             
                
                /*
                 * Exit conditions:
                 * a. retry more than 5 time continuously and if the diff is larger than the previous diff.
                 */
                if (previousDiff < diff) {
                	logger.info("Previous diff was smaller than current diff ---> Retry effort: " + retry);
                	retry++;
                	if (retry == 5){
                    	logger.info("Reached 5 consecutive retries, peer syncing cannot complete");
                    	break;
                	}
                }
                else{
                	retry = 0;
                }
                previousDiff = diff;
            }

            logger.info("Set Dynomite to allow writes only!!!");
            sendCommand("/state/writes_only");
            
            logger.info("Stop Redis' Peer syncing!!!");
            stopPeerSync();
            
            logger.info("Set Dynomite to resuming state to allow writes and flush delayed writes");
            sendCommand("/state/resuming");
            
            //sleep 15s for the flushing to catch up
            sleeper.sleepQuietly(15000);
            logger.info("Set Dynomite to normal state");
            sendCommand("/state/normal");
            
            peerJedis.disconnect();

            if (diff > 0) {
                logger.error("Peer sync can't finish!  Something is wrong.");
                return false;
            }
        }

        return true;
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
           	logger.warn("Warm up takes more than 15 minutes --> moving on");
           	return (long) -1;
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
    
    private boolean sendCommand(String cmd) {
        String url = adminUrl.get() + cmd;
        HttpClient client = new HttpClient();
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());
        
        GetMethod get = new GetMethod(url);
        try {
            int statusCode = client.executeMethod(get);
            if (!(statusCode == 200)) {
                logger.error("Got non 200 status code from " + url);
                return false;
            }
            
            String response = get.getResponseBodyAsString();
            //logger.info("Received response from " + url + "\n" + response);
            
            if (!response.isEmpty()) {
                logger.info("Received response from " + url + "\n" + response);
            } else {
                logger.error("Cannot parse empty response from " + url);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to sendCommand and invoke url: " + url, e);
            return false;
        }
        
        return true;
    }

}