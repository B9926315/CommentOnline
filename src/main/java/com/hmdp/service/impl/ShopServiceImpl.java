package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Planck
 * @since 2023-01-14
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        Shop shop =cacheClient.queryWithLogicalExpire(
                CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }


    /**
     * 更新店铺信息
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺ID不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return Result.ok();
    }
}
