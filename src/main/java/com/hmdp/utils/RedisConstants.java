package com.hmdp.utils;

/**
 * Redis常量类
 * 统一管理Redis的key前缀和TTL时间，方便维护
 *
 * @author Nisson
 */
public class RedisConstants {
    // 登录验证码 key: login:code:手机号
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L; // 验证码有效期2分钟

    // 登录Token key: login:token:UUID
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L; // Token有效期30分钟

    // 缓存空值TTL（防止缓存穿透）
    public static final Long CACHE_NULL_TTL = 2L;

    // 商铺缓存 key: cache:shop:商铺ID
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    // 商铺缓存重建锁 key: lock:shop:商铺ID
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    // 秒杀库存 key: seckill:stock:优惠券ID
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    // 笔记点赞 key: blog:liked:笔记ID (ZSet存储)
    public static final String BLOG_LIKED_KEY = "blog:liked:";

    // Feed流收件箱 key: feed:用户ID (ZSet存储)
    public static final String FEED_KEY = "feed:";

    // 商铺地理位置 key: shop:geo:类型ID (GEO存储)
    public static final String SHOP_GEO_KEY = "shop:geo:";

    // 用户签到 key: sign:用户ID:年月 (Bitmap存储)
    public static final String USER_SIGN_KEY = "sign:";
}
