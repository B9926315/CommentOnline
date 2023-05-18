package com.hmdp.utils;

/**
 * @Author Planck
 * @Date 2023-01-27 - 13:26
 */
public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁持有的超时时间，过期自动释放
     * @return True代表获取锁成功，反之失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();
}
