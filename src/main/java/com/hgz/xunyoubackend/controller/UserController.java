package com.hgz.xunyoubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hgz.xunyoubackend.common.BaseResponse;
import com.hgz.xunyoubackend.common.ErrorCode;
import com.hgz.xunyoubackend.common.Result;
import com.hgz.xunyoubackend.constant.UserConstant;
import com.hgz.xunyoubackend.exception.BusinessException;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.model.request.UserLoginRequest;
import com.hgz.xunyoubackend.model.request.UserRegisterRequest;
import com.hgz.xunyoubackend.model.vo.UserVO;
import com.hgz.xunyoubackend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户接口
 *
 * @author hgz
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return Result.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return Result.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return Result.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return Result.success(userService.getCurrentUser(request));
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        // 仅管理员可查询
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        List<User> userList = userService.searchUsers(username);
        return Result.success(userList);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return Result.success(userList);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pagSize, long pagNum, HttpServletRequest request) {
        //QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //Page<User> userList = userService.page(new Page<>(pagNum, pagSize), queryWrapper);
        //List<User> list = userList.stream().map(user -> userService.getSafeUser(user)).collect(Collectors.toList());
        Page<User> userPage = userService.recommendUsers(pagSize, pagNum, request);
        return Result.success(userPage);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return Result.success(userService.updateUser(user, request));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 逻辑删除
        boolean result = userService.removeById(id);
        return Result.success(result);
    }

    /**
     * 判断是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object userObject = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObject;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        return Result.success(userService.matchUsers(num, currentUser));
    }


}
