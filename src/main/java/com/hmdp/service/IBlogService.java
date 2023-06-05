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
}
