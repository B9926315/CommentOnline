package com.hmdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOCK_KEY_PREFIX;
import static com.hmdp.utils.RedisConstants.THREAD_ID_PREFIX;

/**
 * @Author Planck
 * @Date 2023-01-27 - 13:29
 */
public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = THREAD_ID_PREFIX+Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unLock() {
        //调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_KEY_PREFIX + name),
                THREAD_ID_PREFIX+Thread.currentThread().getId()
                );
    }
    /*@Override
    public void unLock() {
        //获取线程标识
        String threadId = THREAD_ID_PREFIX+Thread.currentThread().getId();
        //获取Redis锁中的标识
        String id = stringRedisTemplate.opsForValue().get(LOCK_KEY_PREFIX + name);
        if (Objects.equals(threadId,id)) {
            stringRedisTemplate.delete(LOCK_KEY_PREFIX + name);
        }
    }*/
}
