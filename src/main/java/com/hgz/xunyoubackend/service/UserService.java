package com.hgz.xunyoubackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hgz.xunyoubackend.model.domain.Tag;
import com.hgz.xunyoubackend.model.domain.User;

import java.util.List;

/**
* @author 32438
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-07-05 06:54:33
*/
public interface UserService extends IService<User> {

    /**
     * 根据标签搜索用户
     * @param tagList
     * @return
     */
    List<User> searchUserByTags(List<Tag> tagList);
}
