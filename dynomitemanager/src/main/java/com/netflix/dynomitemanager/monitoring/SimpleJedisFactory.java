package com.netflix.dynomitemanager.monitoring;

import redis.clients.jedis.Jedis;

/**
 * SimpleJedisFactory create a redis connection with jedis.
 * 
 * @author diegopacheco
 *
 */
public class SimpleJedisFactory implements JedisFactory{
	
	public SimpleJedisFactory() {}
	
	@Override
	public Jedis newInstance() {
		return new Jedis("localhost", 22122);
	}
	
}
