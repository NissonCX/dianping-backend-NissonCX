package com.hmdp.dto;

import lombok.Data;

/**
 * 用户传输对象（数据脱敏）
 * 只包含必要的用户信息，隐藏敏感字段（手机号、密码等）
 * 用于：
 * 1. 登录后返回给前端
 * 2. 存储在Redis中
 * 3. 保存到ThreadLocal中
 *
 * @author Nisson
 */
@Data
public class UserDTO {
    private Long id;         // 用户ID
    private String nickName; // 昵称
    private String icon;     // 头像
}
