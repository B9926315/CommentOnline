package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Planck
 * @Date 2023-01-29 - 14:18
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config=new Config();
        config.useSingleServer().setAddress("redis://43.143.246.214:6379").setPassword("xuanyunyi645134");
        return Redisson.create(config);
    }
}
