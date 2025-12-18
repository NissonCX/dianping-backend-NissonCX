package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Nisson
 * @since 2025-10-01
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}
