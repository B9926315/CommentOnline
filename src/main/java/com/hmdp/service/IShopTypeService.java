package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryList();
}
