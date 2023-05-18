package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.*;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomNumbers(7));
        user.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        save(user);
        return user;
    }
}
