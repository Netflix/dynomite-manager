package com.netflix.dynomitemanager.monitoring;

import redis.clients.jedis.Jedis;

/**
 * Jedis factory to provide a fresh Jedis connection all the time.
 * 
 * @author diegopacheco
 *
 */
public interface JedisFactory {
	public Jedis newInstance();
}
