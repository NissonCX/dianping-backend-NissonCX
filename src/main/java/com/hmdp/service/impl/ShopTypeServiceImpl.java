package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Nisson
 * @since 2025-10-01
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = "cache:shop:type";
        String typeListJson = stringRedisTemplate.opsForValue().get(key);
        if (typeListJson != null) {
            //如果缓存中有数据，则将JSON字符串转换为对象列表后返回
            List<ShopType> shopTypes = JSONUtil.toList(JSONUtil.parseArray(typeListJson), ShopType.class);
            return Result.ok(shopTypes);
        }
        //如果缓存中没有数据，则从数据库中查询ShopType数据
        List<ShopType> shopTypes = getBaseMapper().queryTypeList();
        //将数据写入缓存，并设置过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypes), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypes);
    }
}
