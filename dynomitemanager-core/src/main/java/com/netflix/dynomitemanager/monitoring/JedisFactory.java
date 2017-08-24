package com.netflix.dynomitemanager.monitoring;

import redis.clients.jedis.Jedis;

public interface JedisFactory {
    public Jedis newInstance(String hostname, int port);
}
