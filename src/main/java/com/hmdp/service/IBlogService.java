package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 根据博客ID查询博客详细信息
     * @param id 博客ID
     */
    Result queryBlogById(Long id);

    /**
     * 查询热点博客
     * @param current 当前页码，默认值为1
     */
    Result queryHotBlog(Integer current);

    /**
     * 查询当前登录用户发布过的博客
     * @param current 当前页码，默认值为1
     */
    Result queryMyBlog(Integer current);

    /**
     * 实现用户对博客点赞功能，只能点一次
     * @param id 博客ID
     */
    Result likeBlog(Long id);
    /**
     * 查询点赞该博客的人
     * @param id 博客ID
     */
    Result queryBlogLikes(Long id);

    /**
     * 根据用户ID查询博客
     * @param current 当前页码
     * @param id 用户ID
     */
    Result queryBlogByUserId(Integer current, Long id);
    /**
     * 保存用户发布的探店笔记
     * @param blog 博客实体类
     * @return 通用结果类
     */
    Result saveBlog(Blog blog);
    /**
     * 获取当前登录用户的关注列表的推送消息(基于滚动查询)
     * @param max 时间戳，上次查询的最大值
     * @param offset 偏移量(从max位置开始，偏移几位开始收集结果)
     */
    Result queryBlogOfFollow(Long max, Integer offset);
}
