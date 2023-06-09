package com.hmdp.controller;


import com.hmdp.dto.*;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService userService;
    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        //实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 退出登录
     * @return 无
     */
    @PostMapping("/logout/{token}")
    public Result logout(@PathVariable String token){
        return userService.logout(token);
    }

    /**
     * 获取当前登录用户信息并返回
     */
    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        return userInfoService.info(userId);
    }

    /**
     * 根据ID查询用户
     * @param userId 用户ID
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        return userService.queryUserById(userId);
    }

    /**
     * 为当前登录用户签到
     */
    @PostMapping("/sign")
    public Result userSign(){
        return userService.sign();
    }

    /**
     * 统计当前登录用户连续签到次数
     * @return 天数
     */
    @GetMapping("/sign/count")
    public Result userSignCount(){
        return userService.signCount();
    }
}
