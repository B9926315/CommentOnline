package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        //先从Redis中查询
        String shopTypeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        if (StrUtil.isNotBlank(shopTypeJson)) {
            //可以从Redis中得到，直接返回
            return Result.ok(JSONUtil.toList(shopTypeJson, ShopType.class));
        }
        //Redis中没有，直接查数据库
        List<ShopType> typeList = lambdaQuery().orderByAsc(ShopType::getSort).list();
        if (typeList.isEmpty()) {
            //typeList为空，查询结果为空
            return Result.fail("查询结果为空");
        }
        //typeList不为空，放入缓存，返回结果
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY,
                        JSONUtil.toJsonStr(typeList),
                        CACHE_SHOP_TYPE_TTL,
                        TimeUnit.HOURS);
        return Result.ok(typeList);
    }
}
