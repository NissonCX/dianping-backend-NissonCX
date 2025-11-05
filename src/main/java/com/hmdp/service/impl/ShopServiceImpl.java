package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 商店服务实现类
 * 该类实现了商店相关业务逻辑，包括缓存管理、数据查询和更新等功能
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据ID查询商店信息
     * 提供了两种缓存处理策略：
     * 1. queryWithPassThrough - 缓存穿透解决方案
     * 2. queryWithMutex - 缓存击穿解决方案（使用互斥锁）
     *
     * 当前使用的是queryWithLogicalExpire方法，但该方法存在BUG，
     * 在CacheClient类中没有正确使用keyPrefix参数，导致Redis键构造错误，
     * 使得无法从缓存中获取数据。
     *
     * @param id 商店ID
     * @return 商店信息结果对象
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //用互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //用逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

//    public Shop queryWithLogicalExpire(Long id) {
//        // 构造Redis缓存键
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        // 从Redis缓存中获取商店信息的JSON字符串
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(shopJson)) {
//            return null;
//        }
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            // 未过期，直接返回缓存数据
//            return shop;
//        }
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        if (isLock) {
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    // 重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    unLock(lockKey);
//                }
//            });
//        }
//        return shop;
//    }

    /**
     * 使用互斥锁解决缓存击穿问题
     * 缓存击穿：大量并发请求访问一个正好过期的热点数据
     * 解决方案：使用分布式锁，确保只有一个线程去数据库加载数据
     *
     * @param id 商店ID
     * @return 商店信息，如果不存在则返回null
     */
    //缓存击穿
//    public Shop queryWithMutex(Long id) {
//        // 构造Redis缓存键
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        // 从Redis缓存中获取商店信息的JSON字符串
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 如果缓存中有数据且不为空字符串，则直接解析返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 如果缓存中存储的是空字符串（缓存空值），说明数据库中也不存在该数据，直接返回null，避免缓存穿透
//        if (shopJson != null) {
//            return null;
//        }
//
//        // 缓存重建过程 - 开始
//        // 构造分布式锁的键
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            // 尝试获取分布式锁
//            boolean isLock = tryLock(lockKey);
//            // 如果没有获取到锁，则等待一段时间后重试
//            if (!isLock) {
//                //获取锁失败，则休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//
//            // 获取到锁的线程执行数据库查询
//            shop = getById(id);
//            //模拟重建延迟
//            Thread.sleep(200);
//            // 如果数据库中不存在该商店信息，则将空字符串写入缓存防止缓存穿透
//            if (shop == null) {
//                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//
//            // 将查询到的商店信息转换为JSON字符串并存入Redis缓存
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            unLock(lockKey);
//        }
//        // 释放分布式锁
//        return shop;
//    }

    /**
     * 通过缓存空值解决缓存穿透问题
     * 缓存穿透：查询一个不存在的数据，导致每次请求都打到数据库
     * 解决方案：对不存在的数据也进行缓存，但缓存空值
     *
     * @param id 商店ID
     * @return 商店信息，如果不存在则返回null
     */
    // 缓存穿透
//    public Shop queryWithPassThrough(Long id) {
//        // 构造Redis缓存键
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        // 从Redis缓存中获取商店信息的JSON字符串
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 如果缓存中有数据且不为空字符串，则直接解析返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 如果缓存中存储的是空字符串（缓存空值），说明数据库中也不存在该数据，直接返回null
//        if (shopJson != null) {
//            return null;
//        }
//
//        // 缓存未命中，查询数据库
//        Shop shop = getById(id);
//        // 如果数据库中不存在该商店信息，则将空字符串写入缓存防止缓存穿透
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//
//        // 将查询到的商店信息转换为JSON字符串并存入Redis缓存
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }

//    /**
//     * 尝试获取分布式锁
//     * 使用Redis的SETNX命令实现分布式锁
//     *
//     * @param key 锁的键名
//     * @return 是否成功获取锁
//     */
//    private boolean tryLock(String key) {
//        // 设置锁的值为"1"，过期时间为10秒，防止死锁
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        // 使用BooleanUtil.isTrue()处理可能的null值情况
//        return BooleanUtil.isTrue(flag);
//    }
//
//    /**
//     * 释放分布式锁
//     * 直接删除Redis中的锁键
//     *
//     * @param key 锁的键名
//     */
//    private void unLock(String key) {
//        stringRedisTemplate.delete(key);
//    }
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        Shop shop = getById(id);
        //模拟重建延迟
        Thread.sleep(200);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 更新商店信息
     * 先更新数据库，再删除缓存，保证数据一致性
     *
     * @param shop 商店信息
     * @return 操作结果
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        // 检查商店ID是否为空
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空！");
        }
        // 更新数据库中的商店信息
        updateById(shop);
        // 删除Redis中的缓存，让下次查询时重新加载
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}