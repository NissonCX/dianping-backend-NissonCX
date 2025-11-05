package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopServiceImpl shopService;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() {
        // 使用CountDownLatch确保所有任务执行完毕后再结束测试
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    long id = redisIdWorker.nextId("order");
                    System.out.println("id = " + id);
                }
            } finally {
                // 确保计数器减一，即使出现异常也能正常结束
                latch.countDown();
            }
        };

        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }

        // 等待所有任务执行完毕，设置超时时间避免无限等待
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void testRedis() throws InterruptedException {
        shopService.saveShop2Redis(1L, 10L);
    }

    @Test
    void testSaveShop() {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, shop, 10L, TimeUnit.SECONDS);
    }

}
