package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * 用户信息线程本地存储工具类
 * 使用ThreadLocal保存当前登录用户信息，避免多线程并发问题
 * 每个线程独立存储，互不干扰
 *
 * @author Nisson
 */
public class UserHolder {
    // ThreadLocal：线程隔离的存储容器
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    /**
     * 保存用户信息到当前线程
     * 拦截器中调用，存储登录用户
     */
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    /**
     * 获取当前线程的用户信息
     * Controller中调用，获取登录用户
     */
    public static UserDTO getUser(){
        return tl.get();
    }

    /**
     * 移除当前线程的用户信息
     * 请求结束后调用，防止内存泄漏
     */
    public static void removeUser(){
        tl.remove();
    }
}
