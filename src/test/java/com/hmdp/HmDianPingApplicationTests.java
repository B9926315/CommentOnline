package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 初始阶段需要把所有的商家信息放入Redis缓存，否则前端查不到
     */
    @Test
    void isNotBlankTest() {
        for (int i = 11; i <= 14; i++) {
            Shop shop = shopService.getById(i);
            cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+i,shop,20L, TimeUnit.MINUTES);
        }
    }

    /**
     * 地理坐标排序，运行项目前必须先把店铺信息导入Redis
     */
    @Test
    void loadShopDataWithGeo() {
        //查询店铺信息
        List<Shop> shopList = shopService.list();
        //按照店铺类型typeId分组，typeId一致的放入一个集合
        Map<Long,List<Shop>> map=shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //分批写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //获取店铺类型ID
            Long typeId = entry.getKey();
            String redisGeoKey=SHOP_GEO_KEY+typeId;
            //获取相同类型店铺集合
            List<Shop> shops = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>(shops.size());
            //写入Redis
            for (Shop shop : shops) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(redisGeoKey,locations);
        }
    }

    /**
     * UV统计
     */
    @Test
    void testHyperLog() {
        String[] values=new String[1000];
        int j=0;
        for (int i = 0; i < 1000000; i++) {
            j=i % 1000;
            values[j]="user_"+i;
            if (j==999){
                //每次数组填充满就发，发多次
                stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
            }
        }
        //统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println("count = " + count);
    }
}
