package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author Planck
 * @Date 2023-01-17 - 16:54
 * Redis订单号码生成器
 */
@Component
public class RedisIdWorker {
    //将2023-01-01 00:00:00作为开始时间戳
    private static final Long BEGIN_TIMESTAMP = 1672531200L;
    //序列号位数
    private static final int COUNT_BITS = 32;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix) {
        //生成时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        //生成序列号
        //获取天作为前缀，以冒号隔开
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长得到序列号value
        long count = stringRedisTemplate.opsForValue().increment("incr" + keyPrefix + ":" + date);
        //timeStamp << COUNT_BITS 是将timeStamp转换为2进制后向左移动COUNT_BITS位，得到的10进制数字
        return timeStamp << COUNT_BITS | count;
    }
}
