package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Redis全局ID生成器
 * 基于Redis实现的分布式ID生成器，生成全局唯一的ID
 * ID结构：时间戳(31位) + 序列号(32位) = 63位long型数字
 * <p>
 * 优点：
 * 1. 全局唯一：通过时间戳+Redis自增序列号保证全局唯一性
 * 2. 趋势递增：ID随时间趋势递增，有利于数据库索引性能
 * 3. 高性能：基于Redis单线程原子性操作，保证高并发下的性能
 * 4. 容量大：可以支撑大量ID生成需求
 */
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     * 设置为2022-01-01 00:00:00的UTC时间戳
     * 通过减去开始时间戳，可以减少时间戳位数，腾出更多位给序列号使用
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号位数
     * 用于位运算，将时间戳左移32位，为序列号留出空间
     * long型总共64位，其中1位符号位，31位时间戳，32位序列号
     */
    private static final long COUNT_BITS = 32;

    /**
     * Redis操作模板
     * 用于操作Redis，实现序列号的原子性自增
     */
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成全局唯一ID
     * <p>
     * 算法原理：
     * 1. 获取当前时间戳(秒级)，减去开始时间戳得到相对时间戳
     * 2. 获取当前日期，格式化为yyyyMMdd格式，用于按天统计序列号
     * 3. 使用Redis的INCR命令对key进行原子性自增操作，获取序列号
     * 4. 将时间戳左移32位，与序列号进行按位或运算，得到最终ID
     * <p>
     * Redis Key设计：icr:{keyPrefix}:{date}
     * - icr: id counter record 缩写
     * - keyPrefix: 业务前缀，用于区分不同业务的ID生成需求
     * - date: 日期，格式为yyyyMMdd，用于按天统计序列号，避免序列号过大
     *
     * @param keyPrefix 业务前缀，用于区分不同业务场景的ID生成
     * @return 生成的全局唯一ID
     */
    public long nextId(String keyPrefix) {
        //生成时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        //生成序列号
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //拼接并返回
        return timestamp << COUNT_BITS | count;
    }

    /**
     * 计算开始时间戳的main方法
     * 用于计算指定日期的时间戳，便于设置BEGIN_TIMESTAMP常量
     * <p>
     * 示例：计算2022年1月1日0点0分0秒的UTC时间戳
     */
    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second = " + second);
    }
}