package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.*;
import com.hmdp.entity.*;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.*;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

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
    @Resource
    private IFollowService followService;

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
    /**
     * 根据用户ID查询博客
     * @param current 当前页码
     * @param id 用户ID
     */
    @Override
    public Result queryBlogByUserId(Integer current, Long id) {
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
    /**
     * 保存用户发布的探店笔记
     * @param blog 博客实体类
     * @return 通用结果类
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            return Result.fail("用户未登录");
        }
        Long userId = user.getId();
        blog.setUserId(userId);
        // 保存探店博客
        boolean isSucceed = save(blog);
        if (!isSucceed){
            return Result.fail("保存博客失败");
        }
        //保存博客成功后将其发送给粉丝
        //查询笔记作者的所有粉丝
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId,userId);
        //得到所有粉丝的list集合
        List<Follow> follows = followService.list(queryWrapper);
        //获取时间戳
        long currentTimeMillis = System.currentTimeMillis();
        for (Follow follow : follows) {
            //单个粉丝ID
            Long followUserId = follow.getUserId();
            String feedRedisKey=FEED_KEY+followUserId;
            stringRedisTemplate.opsForZSet().add(feedRedisKey,blog.getId().toString(), currentTimeMillis);
        }
        // 返回id
        return Result.ok(blog.getId());
    }
    /**
     * 获取当前登录用户的关注列表的推送消息(基于滚动查询)
     * @param max 时间戳，上次查询的最大值
     * @param offset 偏移量(从max位置开始，偏移几位开始收集结果)
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取当前登录用户
        UserDTO userCurrent = UserHolder.getUser();
        if (Objects.isNull(userCurrent)){
            return Result.fail("用户未登录");
        }
        Long userCurrentId = userCurrent.getId();
        String feedRedisKey=FEED_KEY+userCurrentId;
        //查询收件箱
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(feedRedisKey, 0, max, offset, 2);
        if (Objects.isNull(typedTuples) || typedTuples.isEmpty()){
            return Result.ok();
        }
        //解析数据：blogId、minTime(时间戳)
        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        //要找到offset(偏移量)的值，因为已经有他自己一样，所以初值为一，每当找到与最后一个值相同的元素，os+1
        int os=1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            //将获取的ID放入ArrayList集合
            ids.add(Long.valueOf(tuple.getValue()));
            //获取最小的时间戳
            long time = tuple.getScore().longValue();
            if (time==minTime){
                //这一次接收到的值与上一次一样，说明os可以加一
                os++;
            }else {
                /*
                这一次的值与上一次不一样，计数器重置。
                （比如：9 8 8 6 2 1 1中，两个8计数器值为2，但是8不是最小值，所以遍历到6的时候计数器需要重置）
                */
                minTime= time;
                os=1;
            }
        }
        //根据博客ID查询博客并封装数据
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Blog blog : blogs) {
            //查询blog有关的用户
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }
}