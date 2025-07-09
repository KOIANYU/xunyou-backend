package com.hgz.xunyoubackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hgz.xunyoubackend.mapper.UserMapper;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 *
 * @author hgz
 */
@Slf4j
@Component
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 先设置重点用户
     */
    //private List<Long> mainUserIdList = Arrays.asList(7L);

    private int pageSize = 20;

    private int totalPages = 20;

    /**
     * 方案一：缓存预热所有用户信息
     * 每天23:59执行缓存预热
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void CacheRecommendUser() {
        // 动态获取最近 7天 活跃用户 ID 列表
        List<Long> activeUserIds = userService.getActiveUserIdsLast7Days();

        if (activeUserIds == null || activeUserIds.isEmpty()) {
            log.warn("No active users found in the last 7 days.");
            return;
        }

        for (Long userId : activeUserIds) {
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();

            // 预热所有页面
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                String redisKey = String.format("xunyou:user:recommend:%s:page%s", userId, pageNum);
                Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);

                try {
                    valueOperations.set(redisKey, userPage, 360, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("redis set key error: ", e);
                }
            }
        }
    }

    /**
     * 方案二：缓存预热热门用户信息
     *
     * @author hgz
     */
    //@Scheduled(cron = "0 59 23 * * ?")
    public void CacheRecommendHotUser() {
        // 动态获取最近 7天 活跃用户 ID 列表
        List<Long> activeUserIds = userService.getActiveUserIdsLast7Days();


    }

}
