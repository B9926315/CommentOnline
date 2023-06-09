package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;
    /**
     * 当前登录用户对某博主进行关注或取关
     * @param followUserId 被关注人的用户ID
     * @param isFollow 关注标识符
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,@PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    /**
     * 判断当前登录用户是否关注该用户
     * @param followUserId 用于判断被关注用户ID
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }
    /**
     * 查询共同关注
     * @param id 用户ID(不是当前登录用户ID)
     */
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id") Long id){
        return followService.commonFollow(id);
    }
}
