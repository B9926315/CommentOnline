package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @Author Planck
 * @Date 2023-05-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private IBlogService blogService;

    /**
     * 保存用户发布的探店笔记
     * @param blog 博客实体类
     * @return 通用结果类
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 用户对博客进行点赞
     * @param id 博客ID
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /**
     * 查询当前登录用户发布过的博客
     * @param current 当前页码
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBlog(current);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 通过ID查询博客
     * @param id 博客ID
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id")Long id){
        return blogService.queryBlogById(id);
    }

    /**
     * 查询点赞该博客的人
     * @param id 博客ID
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id")Long id){
        return blogService.queryBlogLikes(id);
    }

    /**
     * 根据用户ID查询博客
     * @param current 当前页码
     * @param id 用户ID
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        return blogService.queryBlogByUserId(current,id);
    }

    /**
     * 获取当前登录用户的关注列表的推送消息(基于滚动查询)
     * @param max 时间戳，上次查询的最大值
     * @param offset 偏移量(从max位置开始，偏移几位开始收集结果，默认值为零)
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset",defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }
}
