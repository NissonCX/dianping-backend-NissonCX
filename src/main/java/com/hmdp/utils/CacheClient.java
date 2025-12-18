package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * 缓存工具类
 * 封装了多种缓存策略，解决缓存穿透、击穿、雪崩问题
 * 提供统一的缓存操作接口，简化Service层代码
 *
 * @author Nisson
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    // 缓存重建线程池（10个线程）
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 写入缓存（普通方式）
     * @param key 缓存key
     * @param value 缓存值
     * @param time 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 写入缓存（逻辑过期方式）
     * 用于解决缓存击穿，缓存永不过期，但存储过期时间字段
     * @param key 缓存key
     * @param value 缓存值
     * @param time 逻辑过期时间
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis（无TTL，永不过期）
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透解决方案：缓存空值
     * 适用场景：防止恶意查询不存在的数据
     *
     * @param keyPrefix key前缀
     * @param id 查询ID
     * @param type 返回值类型
     * @param dbFallback 数据库查询函数（查不到缓存时的降级策略）
     * @param time 缓存时间
     * @param unit 时间单位
     * @return 查询结果
     */
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.缓存命中，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值（防止缓存穿透）
        if (json != null) {
            // 是空值（""），返回null
            return null;
        }

        // 4.缓存未命中，查询数据库（调用传入的函数）
        R r = dbFallback.apply(id);
        // 5.数据库也不存在
        if (r == null) {
            // 将空值写入Redis（TTL较短，2分钟）
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6.数据库存在，写入Redis缓存
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 缓存击穿解决方案：逻辑过期 + 互斥锁
     * 适用场景：热点key，对一致性要求不高
     * 优点：性能高（始终返回数据，后台异步更新）
     * 缺点：可能返回过期数据
     *
     * @param keyPrefix key前缀
     * @param id 查询ID
     * @param type 返回值类型
     * @param dbFallback 数据库查询函数
     * @param time 逻辑过期时间
     * @param unit 时间单位
     * @return 查询结果
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从Redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.不存在，直接返回null（逻辑过期方案不处理缓存未命中）
            return null;
        }
        // 4.缓存命中，反序列化对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否逻辑过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回数据
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock){
            // 6.3.成功，开启独立线程，异步重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存（逻辑过期）
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4.返回过期的数据（旧数据也比没数据好）
        return r;
    }

    /**
     * 缓存击穿解决方案：互斥锁
     * 适用场景：对一致性要求高的场景
     * 优点：强一致性，线程安全
     * 缺点：性能较低（其他线程需等待）
     *
     * @param keyPrefix key前缀
     * @param id 查询ID
     * @param type 返回值类型
     * @param dbFallback 数据库查询函数
     * @param time 缓存时间
     * @param unit 时间单位
     * @return 查询结果
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从Redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.缓存命中，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 是空值，返回null
            return null;
        }

        // 4.缓存未命中，实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取锁成功
            if (!isLock) {
                // 4.3.失败，休眠50ms后重试（递归调用）
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.成功，查询数据库
            r = dbFallback.apply(id);
            // 5.数据库不存在
            if (r == null) {
                // 缓存空值（防止缓存穿透）
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6.数据库存在，写入缓存
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            unlock(lockKey);
        }
        // 8.返回结果
        return r;
    }

    /**
     * 尝试获取锁
     * @param key 锁的key
     * @return true=获取成功，false=获取失败
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag); // 防止自动拆箱空指针
    }

    /**
     * 释放锁
     * @param key 锁的key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
