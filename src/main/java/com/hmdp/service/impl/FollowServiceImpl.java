package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_USER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    /**
     * 判断followUserId对应的用户是否被当前登录用户关注
     * @param followUserId 用于判断被关注用户ID，
     */
    @Override
    public Result isFollow(Long followUserId) {
        //获取登录用户ID
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            throw new RuntimeException("用户未登录");
        }
        Long userId = user.getId();
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId,followUserId);
        queryWrapper.eq(Follow::getUserId,userId);
        int count = count(queryWrapper);
        //count>0则代表当前登录用户已关注该博主
        return Result.ok(count>0);
    }
    /**
     * 当前登录用户对某博主进行关注或取关
     * @param followUserId 被关注人的用户ID,当前登录用户要对他进行关注或者取关
     * @param isFollow 关注标识符，TRUE要进行关注，false要进行取关
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取登录用户ID
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            throw new RuntimeException("用户未登录");
        }
        Long userId = user.getId();
        //将当前登录用户设置为Redis关注列表的key
        String redisUserKey=FOLLOW_USER_KEY+userId;
        if (isFollow){
            //要进行关注操作，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSucceed = save(follow);

            if (isSucceed){
                //将关注用户ID放入set集合
                stringRedisTemplate.opsForSet().add(redisUserKey,followUserId.toString());
            }
        }else {
            //要进行取关操作，删除数据
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getFollowUserId,followUserId);
            queryWrapper.eq(Follow::getUserId,userId);
            boolean isRemove = remove(queryWrapper);
            if (isRemove){
                //将关注用户ID从set集合移除
                //将当前登录用户设置为key
               // String redisUserKey=FOLLOW_USER_KEY+userId;
                stringRedisTemplate.opsForSet().remove(redisUserKey,followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询共同关注
     * @param id 用户ID(不是当前登录用户ID)
     */
    @Override
    public Result commonFollow(Long id) {
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            return Result.fail("用户未登录");
        }
        Long userId = user.getId();
        String key1=FOLLOW_USER_KEY+userId;
        String key2=FOLLOW_USER_KEY+id;
        //获取共同关注的列表
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        //如果set为空或者为null，则代表两人没有共同关注
        if (Objects.isNull(intersect) || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析ID集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(userTemporary -> BeanUtil.copyProperties(userTemporary, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
