package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录校验拦截器（第二层拦截器）
 * 作用：拦截需要登录的接口，校验用户是否登录
 * 执行顺序：在RefreshTokenInterceptor之后执行
 *
 * @author Nisson
 */
public class LoginIntercepter implements HandlerInterceptor {

    /**
     * 请求前执行
     * 校验ThreadLocal中是否有用户信息
     * 无用户信息则返回401（未登录）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断ThreadLocal中是否有用户（由RefreshTokenInterceptor存入）
        if (UserHolder.getUser() == null) {
            // 未登录，返回401状态码
            response.setStatus(401);
            return false; // 拦截
        }

        // 已登录，放行
        return true;
    }

}
