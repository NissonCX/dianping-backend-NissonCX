package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * Redisson是Redis的Java客户端，提供分布式锁、分布式集合等功能
 * 优势：
 * 1. 看门狗机制：自动续期，防止业务执行时间过长导致锁提前释放
 * 2. RedLock算法：支持Redis集群模式下的分布式锁
 * 3. 可重入锁：同一线程可多次获取锁
 *
 * @author Nisson
 */
@Configuration
public class RedissonConfig {
    /**
     * 创建RedissonClient Bean
     * 单机模式配置
     */
    @Bean
    public RedissonClient redissonClient() {
        // 创建配置对象
        Config config = new Config();
        // 单机模式配置（也可配置集群、哨兵模式）
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        // 创建Redisson客户端
        return Redisson.create(config);
    }
}
