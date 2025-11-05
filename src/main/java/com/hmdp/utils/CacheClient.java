package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存客户端工具类
 * 提供统一的缓存操作接口，包括：
 * 1. 基础缓存读写
 * 2. 带逻辑过期的缓存读写
 * 3. 缓存穿透解决方案
 * 4. 缓存击穿解决方案（逻辑过期+互斥锁）
 */
@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 设置缓存数据（物理过期）
     *
     * @param key   缓存键
     * @param value 缓存值（会自动序列化为JSON字符串）
     * @param time  过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 设置缓存数据（逻辑过期）
     * 数据永不过期，通过内部的expireTime字段判断是否过期
     *
     * @param key   缓存键
     * @param value 缓存值（会自动序列化为JSON字符串）
     * @param time  逻辑过期时间
     * @param unit  时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透解决方案 - 缓存空值
     * 查询数据时，先查缓存，缓存未命中查数据库，数据库未命中将空值写入缓存
     *
     * @param keyPrefix  缓存键前缀
     * @param id         数据ID
     * @param type       返回值类型
     * @param dbFallback 数据库查询函数
     * @param time       缓存过期时间
     * @param unit       时间单位
     * @param <R>        返回值类型
     * @param <ID>       ID类型
     * @return 查询结果
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 构造Redis缓存键
        String key = keyPrefix + id;
        // 从Redis缓存中获取商店信息的JSON字符串
        String json = stringRedisTemplate.opsForValue().get(key);
        // 如果缓存中有数据且不为空字符串，则直接解析返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 如果缓存中存储的是空字符串（缓存空值），说明数据库中也不存在该数据，直接返回null
        if (json != null) {
            return null;
        }

        // 缓存未命中，查询数据库
        R r = dbFallback.apply(id);
        // 如果数据库中不存在该商店信息，则将空字符串写入缓存防止缓存穿透
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 将查询到的商店信息转换为JSON字符串并存入Redis缓存
        this.set(key, r, time, unit);
        return r;
    }


    /**
     * 用于执行缓存重建任务的线程池
     * 固定大小为10的线程池，用于异步执行缓存重建任务
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 缓存击穿解决方案 - 逻辑过期+互斥锁
     * 数据永不过期，通过逻辑过期时间和互斥锁解决缓存击穿问题
     * <p>
     * 注意：当前方法实现存在BUG，第107行代码硬编码使用了RedisConstants.CACHE_SHOP_KEY，
     * 而没有使用传入的keyPrefix参数，这会导致使用该方法时无法正确构造Redis键。
     * 应该修改为：String key = keyPrefix + id;
     *
     * @param keyPrefix  缓存键前缀（注意：当前实现中未正确使用该参数）
     * @param id         数据ID
     * @param type       返回值类型
     * @param dbFallback 数据库查询函数
     * @param time       逻辑过期时间
     * @param unit       时间单位
     * @param <R>        返回值类型
     * @param <ID>       ID类型
     * @return 查询结果
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 构造Redis缓存键
        String key = keyPrefix + id;
        // 从Redis缓存中获取商店信息的JSON字符串
        String json = stringRedisTemplate.opsForValue().get(key);
        // 如果Redis中没有缓存数据，应该从数据库查询并重建缓存
        // BUG: 原代码在此处直接返回null，不会从数据库查询数据(已修复)
        if (StrUtil.isBlank(json)) {
            // 从数据库查询数据
            R r = dbFallback.apply(id);
            // 如果数据库中存在数据，则重建缓存
            if (r != null) {
                this.setWithLogicalExpire(key, r, time, unit);
            }
            // 返回查询结果
            return r;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，直接返回缓存数据
            return r;
        }
        String lockKey = keyPrefix + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return r;
    }

    /**
     * 尝试获取分布式锁
     * 使用Redis的SETNX命令实现分布式锁
     *
     * @param key 锁的键名
     * @return 是否成功获取锁
     */
    private boolean tryLock(String key) {
        // 设置锁的值为"1"，过期时间为10秒，防止死锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // 使用BooleanUtil.isTrue()处理可能的null值情况
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放分布式锁
     * 直接删除Redis中的锁键
     *
     * @param key 锁的键名
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}