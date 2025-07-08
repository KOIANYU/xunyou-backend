package com.hgz.xunyoubackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hgz.xunyoubackend.model.domain.Tag;
import com.hgz.xunyoubackend.model.domain.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hgz
* @description 针对表【user(用户)】的数据库操作Service
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request 用户登录状态
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户信息脱敏
     *
     * @param user
     * @return
     */
    User getSafeUser(User user);

    /**
     * 查询用户信息
     *
     * @param username
     * @return
     */
    List<User> searchUsers(String username);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagList
     * @return
     */
    List<User> searchUserByTags(List<String> tagList);

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    int updateUser(User user, HttpServletRequest request);

    User getCurrentUser(HttpServletRequest request);

}
