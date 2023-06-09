package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-24
 */
public interface IUserInfoService extends IService<UserInfo> {

    Result info(Long userId);
}
