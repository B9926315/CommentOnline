package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * @author Planck
 * @since 2023-01-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    private IVoucherOrderService proxy;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT =new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //创建阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR=Executors.newSingleThreadExecutor();
    //此注解表示，类加载完成后，立即加载此方法
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while (true){
            try {
                VoucherOrder voucherOrder = orderTasks.take();
                handleVoucherOrder(voucherOrder);
            } catch (Exception e) {
                log.error("处理订单异常："+e);
            }
        }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            //获取失败，返回错误信息
            log.error("不允许重复下单");
            return;
        }
        try {
            //为防止事务失效，获取事务代理对象（事务）
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 用户抢秒杀券
     * @param voucherId 秒杀券ID
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        //判断结果是否为零
        int r = result.intValue();
        if (r!=0) {
            //不为零，无购买资格
            return Result.fail(r==1?"库存不足":"不能重复下单");
        }
        long orderId = redisIdWorker.nextId("order");
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        //代金券ID
        voucherOrder.setVoucherId(voucherId);
        orderTasks.add(voucherOrder);
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        log.info("订单ID："+orderId);
        return Result.ok(orderId);

    }
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券,他们是共享ID的，所以可以通用
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //秒杀尚未开始
            return Result.fail("秒杀尚未开始!");
        }
        //判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀已经结束
            return Result.fail("秒杀已经结束!");
        }
        //判断库存是否充足
        if (voucher.getStock()<1) {
            return Result.fail("秒杀券已售罄！");
        }
        //加锁
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        //SimpleRedisLock lock=new SimpleRedisLock("order:"+userId,stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            //获取失败，返回错误信息
            return Result.fail("每人只能下一单！");
        }
        try {
            //为防止事务失效，获取事务代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }

    }*/
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //一人一单
        Long userId = voucherOrder.getUserId();
        int count = lambdaQuery().eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId()).count();
        if (count>0) {
            //该用户已经下单，不能再购
            return;
        }
        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .gt("stock",0)
                .eq("voucher_id", voucherOrder.getVoucherId())
                .update();
        if (!success) {
            return;
        }
        //创建订单

        save(voucherOrder);
    }
}
