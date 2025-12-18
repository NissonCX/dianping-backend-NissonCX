package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Redis数据封装类（逻辑过期方案）
 * 用于解决缓存击穿问题
 *
 * 逻辑过期方案：
 * - Redis中的数据永不过期（无TTL）
 * - 但存储一个逻辑过期时间字段
 * - 查询时判断逻辑过期时间，过期则后台异步更新
 * - 优点：始终返回数据（旧数据也比没数据好）
 *
 * @author Nisson
 */
@Data
public class RedisData {
    private LocalDateTime expireTime; // 逻辑过期时间
    private Object data;              // 实际数据
}
