package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
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
            return Result.ok(typeListJson);
        }
        //如果缓存中没有数据，则从数据库中查询ShopType数据
        List<ShopType> typeList = getBaseMapper().queryTypeList();
        //将数据写入缓存
        stringRedisTemplate.opsForValue().set(key, typeList.toString());
        return Result.ok(typeList);
    }
}