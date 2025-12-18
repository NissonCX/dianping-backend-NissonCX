package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 黑马点评项目启动类
 *
 * 项目简介：
 * 类似大众点评的本地生活服务平台，提供商户查询、探店笔记、优惠券秒杀等功能
 *
 * 技术栈：
 * - Spring Boot 2.7.14
 * - MyBatis-Plus 3.4.3
 * - Redis（缓存、分布式锁、消息队列）
 * - Redisson（分布式锁）
 * - MySQL 5.7+
 *
 * 核心功能：
 * 1. 短信验证码登录（Redis + Token）
 * 2. 商铺缓存优化（解决缓存三大问题）
 * 3. 优惠券秒杀（异步下单、防超卖）
 * 4. Feed流推送（ZSet + 滚动分页）
 * 5. 附近商铺搜索（GEO）
 * 6. 用户签到（Bitmap）
 *
 * @author Nisson
 */
@MapperScan("com.hmdp.mapper")  // 扫描Mapper接口
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)  // 开启AOP代理，暴露代理对象
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }


}
