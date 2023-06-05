package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据博客ID查询博客详细信息
     *
     * @param id 博客ID
     */
    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (Objects.isNull(blog)) {
            Result.fail("笔记不存在");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 判断当前博客是否被点赞过
     */
    private void isBlogLiked(Blog blog) {
        //1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            //用户未登录,无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        //2.判断当前登录用户是否已经点赞
        String redisKey = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(redisKey, String.valueOf(userId));
        blog.setIsLike(Objects.nonNull(score));

    }

    /**
     * 将用户信息封装到博客中
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 查询热点博客
     *
     * @param current 当前页码，默认值为1
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 查询当前登录用户发布过的博客
     *
     * @param current 当前页码，默认值为1
     */
    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 实现用户对博客点赞功能，只能点一次
     *
     * @param id 博客ID
     */
    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2.判断当前登录用户是否已经点赞
        String redisKey = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(redisKey, String.valueOf(userId));
        if (Objects.isNull(score)) {
            //3.未点赞，可以点赞。数据库点赞数+1，保存用户到Redis的set集合
            boolean isSucceed = update().setSql("liked=liked+1").eq("id", id).update();
            if (isSucceed) {
                stringRedisTemplate.opsForZSet().add(redisKey, userId.toString(),System.currentTimeMillis());
            }
        } else {
            //4.已点赞，则取消点赞。数据库点赞数-1，删除Redis的set集合中用户信息
            boolean isSucceed = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSucceed) {
                stringRedisTemplate.opsForZSet().remove(redisKey, userId.toString());
            }
        }
        return Result.ok();
    }
    /**
     * 查询点赞该博客的人
     * @param id 博客ID
     */
    @Override
    public Result queryBlogLikes(Long id) {
        //查询点赞top5的用户
        String redisKey = BLOG_LIKED_KEY + id;
        Set<String> top5Id = stringRedisTemplate.opsForZSet().range(redisKey, 0, 4);
        if (Objects.isNull(top5Id) || top5Id.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5Id.stream().map(Long::valueOf).collect(Collectors.toList());
        String idsString = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids)
                //根据传入ID的顺序排序
                .last("ORDER BY FIELD(id,"+idsString+")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}