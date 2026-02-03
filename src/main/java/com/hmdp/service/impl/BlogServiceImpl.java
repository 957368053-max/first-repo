package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
@Autowired
private UserServiceImpl userService;
@Autowired
private StringRedisTemplate stringRedisTemplate;
@Autowired
private IFollowService followService;
    @Autowired
    private IFollowService iFollowService;

    @Override
    public Result queryBlogLikes(Long id) {

        String key = "blog:liked:" + id;
        Set<String> range = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (range == null || range.isEmpty()) {
            return Result.ok();
        }
        List<Long> ids   = range.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userdio = userService.listByIds(ids)
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userdio);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            // 查询笔记作者的id
           return Result.fail("笔记保存失败！");
            } Long userId = blog.getUserId();

            List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
            for (Follow follow : follows) {
                Long followId = follow.getUserId();
                String key = "blog:feed:" + followId;
                stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());



        }return Result.ok(blog.getId());

    }

    @Override
    public Result queryBlogById(Long id) {


        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        Long blogId = blog.getId();
        Long userId = UserHolder.getUser().getId();
        User user = userService.getById( blogId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        String key = "blog:liked:" +  blogId;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!= null);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {

        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if ( score== null) {
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(),System.currentTimeMillis());
            }
        } else {
            boolean isSuccess2 = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess2) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }

        }
        return Result.ok();

    }
}
