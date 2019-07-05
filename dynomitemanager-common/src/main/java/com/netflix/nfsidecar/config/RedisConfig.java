package com.netflix.nfsidecar.config;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;

@Configuration(prefix = "redis")
public interface RedisConfig {
    @DefaultValue("/apps/nfredis/conf/redis.conf")
    @PropertyName(name = "confPath")
    String getConfPath();

    @DefaultValue("127.0.0.1")
    @PropertyName(name = "address")
    String getAddress();

    @DefaultValue("22122")
    @PropertyName(name = "port")
    int getPort();

    @DefaultValue("/apps/nfredis/bin/launch_nfredis.sh")
    @PropertyName(name = "startScript")
    String getStartScript();

    @DefaultValue("/apps/nfredis/bin/kill_redis.sh")
    @PropertyName(name = "stopScript")
    String getStopScript();
}
