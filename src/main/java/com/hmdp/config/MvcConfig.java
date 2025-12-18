package com.hmdp.config;

import com.hmdp.utils.LoginIntercepter;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MVC配置类
 * 配置拦截器，实现登录校验和Token刷新
 *
 * @author Nisson
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加拦截器
     * 执行顺序：order值越小越先执行
     * 1. RefreshTokenInterceptor（order=0）：拦截所有请求，刷新Token
     * 2. LoginInterceptor（order=1）：拦截需要登录的请求，校验登录状态
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录校验拦截器（order=1，后执行）
        registry.addInterceptor(new LoginIntercepter())
                .excludePathPatterns(
                        "/user/code",      // 发送验证码
                        "/user/login",     // 登录
                        "/blog/hot",       // 热门笔记
                        "/shop/**",        // 商铺查询
                        "/shop-type/**",   // 商铺类型
                        "/voucher/**"      // 优惠券查询
                )
                .order(1);

        // Token刷新拦截器（order=0，先执行）
        // 拦截所有请求，刷新Token有效期
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
    }
}
