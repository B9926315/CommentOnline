package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author Planck
 * @Date 2023-01-13 - 21:41
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            //没有登录，拦截
            response.setStatus(401);
            return false;
        }
        //已登录，放行
        return true;
    }
}
