package com.hmdp.utils;

import cn.hutool.core.lang.UUID;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 5L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 24L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop-type";
    public static final Long CACHE_SHOP_TYPE_TTL = 24L;

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FOLLOW_USER_KEY = "follows:";
    //博主发送博客时候向粉丝信箱推送内容
    public static final String FEED_KEY = "feed:";
    //附近商铺按地理坐标排序
    public static final String SHOP_GEO_KEY = "shop:geo:";
    //用户签到
    public static final String USER_SIGN_KEY = "sign:";
//分布式锁
    public static final String LOCK_KEY_PREFIX = "lock:";
    public static final String THREAD_ID_PREFIX = UUID.randomUUID().toString(true)+"-";
}
