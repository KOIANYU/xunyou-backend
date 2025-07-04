package com.hgz.xunyoubackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.mapper.UserMapper;
import com.hgz.xunyoubackend.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author 32438
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-07-05 06:54:33
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




