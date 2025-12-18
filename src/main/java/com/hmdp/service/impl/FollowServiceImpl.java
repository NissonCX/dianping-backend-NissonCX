package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注服务实现类
 * 核心功能：
 * 1. 关注/取关用户
 * 2. 查询是否关注
 * 3. 查询共同关注（基于Redis Set交集）
 *
 * @author Nisson
 * @since 2025-10-01
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    /**
     * 关注或取关用户
     * 同时维护MySQL和Redis数据
     * Redis Set用于快速查询共同关注
     *
     * @param followUserId 被关注的用户ID
     * @param isFollow true=关注，false=取关
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId; // Redis key: follows:当前用户ID

        // 2.判断是关注还是取关
        if (isFollow) {
            // 关注：新增数据到MySQL和Redis
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 将关注用户ID存入Redis Set（用于共同关注查询）
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 取关：从MySQL和Redis删除数据
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 从Redis Set中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断是否关注某用户
     * @param followUserId 被关注用户ID
     * @return true=已关注，false=未关注
     */
    @Override
    public Result isFollow(Long followUserId) {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询数据库
        Integer count = query()
                .eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count();
        // 3.返回结果
        return Result.ok(count > 0);
    }

    /**
     * 查询共同关注
     * 利用Redis Set的交集功能，高效查询
     *
     * @param id 目标用户ID
     * @return 共同关注的用户列表
     */
    @Override
    public Result followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key1 = "follows:" + userId; // 当前用户关注列表
        String key2 = "follows:" + id;     // 目标用户关注列表

        // 2.求两个Set的交集（Redis SINTER命令）
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无共同关注
            return Result.ok(Collections.emptyList());
        }

        // 3.解析用户ID集合
        List<Long> ids = intersect.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 4.查询用户详细信息
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(users);
    }
}
