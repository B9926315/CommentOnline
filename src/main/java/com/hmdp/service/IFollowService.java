package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
public interface IFollowService extends IService<Follow> {
    /**
     * 判断当前登录用户是否关注该用户
     * @param followUserId 用于判断被关注用户ID
     */
    Result isFollow(Long followUserId);
    /**
     * 当前登录用户对某博主进行关注或取关
     * @param followUserId 被关注人的用户ID
     * @param isFollow 关注标识符
     */
    Result follow(Long followUserId, Boolean isFollow);
    /**
     * 查询共同关注
     * @param id 用户ID(不是当前登录用户ID)
     */
    Result commonFollow(Long id);
}
