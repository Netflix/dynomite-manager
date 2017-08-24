package com.netflix.dynomitemanager.monitoring;

import redis.clients.jedis.Jedis;

public class SimpleJedisFactory implements JedisFactory {

    public SimpleJedisFactory() {
    }

    @Override
    public Jedis newInstance(String hostname, int port) {
        return new Jedis(hostname, port);
    }

}