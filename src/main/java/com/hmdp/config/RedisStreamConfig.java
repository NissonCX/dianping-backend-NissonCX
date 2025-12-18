package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;

/**
 * Redis Stream 配置类
 * 用于初始化秒杀订单的消息队列和消费者组
 *
 * @author Nisson
 */
@Slf4j
@Configuration
public class RedisStreamConfig {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY = "stream.orders";
    private static final String GROUP_NAME = "g1";

    /**
     * 初始化Stream消息队列和消费者组
     * 在项目启动时自动执行
     */
    @PostConstruct
    public void initStreamGroup() {
        try {
            // 检查Stream是否存在，不存在则创建
            Boolean hasKey = stringRedisTemplate.hasKey(STREAM_KEY);
            if (Boolean.FALSE.equals(hasKey)) {
                // 创建Stream并添加一条初始消息（创建Stream的唯一方式）
                // 使用XGROUP CREATE ... MKSTREAM可以同时创建Stream
                log.info("Stream [{}] 不存在，正在创建...", STREAM_KEY);
            }

            // 创建消费者组，如果已存在会抛出异常
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("消费者组 [{}] 创建成功", GROUP_NAME);
        } catch (Exception e) {
            // 消费者组已存在或Stream不存在
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组 [{}] 已存在，跳过创建", GROUP_NAME);
            } else if (e.getMessage() != null && e.getMessage().contains("no such key")) {
                // Stream不存在，尝试使用MKSTREAM创建
                try {
                    stringRedisTemplate.execute((connection) -> {
                        connection.xGroupCreate(
                                STREAM_KEY.getBytes(),
                                GROUP_NAME,
                                ReadOffset.from("0"),
                                true  // MKSTREAM - 如果Stream不存在则创建
                        );
                        return null;
                    }, true);
                    log.info("Stream [{}] 和消费者组 [{}] 创建成功", STREAM_KEY, GROUP_NAME);
                } catch (Exception ex) {
                    log.warn("创建Stream和消费者组时发生异常: {}", ex.getMessage());
                }
            } else {
                log.warn("初始化Stream消费者组时发生异常: {}", e.getMessage());
            }
        }
    }
}

