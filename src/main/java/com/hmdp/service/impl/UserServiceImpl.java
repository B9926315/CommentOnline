package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.*;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送验证码
     * @param phone 前端传回的手机号
     * @return 无返回值
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号无效，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("验证码：{}",code);
        //保存验证码
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL  , TimeUnit.MINUTES);
        return Result.ok();
    }

    /**
     * 登录功能
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号无效，返回错误信息
            return Result.fail("手机号格式错误");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String formCode = loginForm.getCode();
        if (cacheCode==null || !cacheCode.equals(formCode)){
            return Result.fail("验证码错误");
        }
        //查询该用户是否存在
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            //用户不存在，创建新用户
            user=createUserWithPhone(phone);
        }
        //随机生成token值，保存到Redis
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                           .setIgnoreNullValue(true)
        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //设置Token有效期30 Min
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     * 退出登录功能
     */
    @Override
    public Result logout(String token) {
        //获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            throw new RuntimeException("用户未登录");
        }
        UserHolder.removeUser();
        stringRedisTemplate.delete(LOGIN_USER_KEY+token);
        return Result.ok();
    }

    /**
     * 根据ID查询用户
     * @param userId 用户ID
     */
    @Override
    public Result queryUserById(Long userId) {
        // 查询详情
        User user = getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }
    /**
     * 为当前登录用户签到
     */
    @Override
    public Result sign() {
        //获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)) {
            return Result.fail("用户为登录");
        }
        Long userId = user.getId();
        //获取日期
        LocalDateTime nowTime = LocalDateTime.now();
        //拼接key
        String keySuffix = nowTime.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String redisKeyBigMap=USER_SIGN_KEY+userId+keySuffix;
        //获取今天是本月第几天
        int dayOfMonth = nowTime.getDayOfMonth();
        //写入Redis
        stringRedisTemplate.opsForValue().setBit(redisKeyBigMap,dayOfMonth-1,true);
        return Result.ok();
    }
    /**
     * 统计当前登录用户连续签到次数
     * @return 天数
     */
    @Override
    public Result signCount() {
        //获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)) {
            return Result.fail("用户为登录");
        }
        Long userId = user.getId();
        //获取日期
        LocalDateTime nowTime = LocalDateTime.now();
        //拼接key
        String keySuffix = nowTime.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String redisKeyBigMap=USER_SIGN_KEY+userId+keySuffix;
        //获取今天是本月第几天
        int dayOfMonth = nowTime.getDayOfMonth();
        //获取签到记录是一个十进制数字，循环遍历
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                redisKeyBigMap,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (Objects.isNull(result) || result.isEmpty()){
            return Result.ok(0);
        }
        //得到签到结果的十进制数字
        Long signNumber = result.get(0);
        if (Objects.isNull(signNumber) || signNumber==0){
            //签到结果不存在或者为0
            return Result.ok(0);
        }
/*
循环遍历得到的十进制数字，使其二进制位的每一位都和1做与运算，得到最后一个bit位。
如果最后一个bit位为0，则循环结束，连续签到次数为0。
如果最后一个bit位不为0，则说明这一天已经签到，计数器加一，且将数字右移，抛弃最后一个bit位，继续进行下一个bit位运算
 */
        //设置计数器
        int count=0;
        while (true) {
            if ((signNumber & 1)==0){
                //最后一个bit位为0，未签到，结束
                break;
            }else {
                //已签到
                count++;
            }
            //右移一位并赋值给原数字
            signNumber >>>=1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomNumbers(7));
        user.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        save(user);
        return user;
    }
}
