package com.hmdp;

import cn.hutool.core.util.StrUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.Collator;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private IShopService shopService;
    @Test
    void isNotBlankTest() {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1L,shop,20L, TimeUnit.MINUTES);
    }

    @Test
    void nameTest() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        String[] arr={"安徽","咸阳","重庆","中国"};
        List<String> list= Arrays.asList(arr);
        Collections.sort(list,collator);
        System.out.println(list);
    }
}
