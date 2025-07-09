package com.hgz.xunyoubackend.once;

import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InsertUserInfoTest {

    @Resource
    private UserService userService;

    /**
     * 批量插入用户信息
     */
    @Test
    void doInsertUserInfo() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int NUM = 10000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < NUM; i++) {
            User user = new User();
            user.setUsername("用户测试数据");
            user.setUserAccount("fakehgz");
            user.setAvatarUrl("https://crowdfundiing.oss-cn-beijing.aliyuncs.com/bc592156-4407-44d8-b964-da9e070cfb98.JPG");
            user.setGender(0);
            user.setUserPassword("38d2dbb0c93c784bb0f3de960231a477");
            user.setPhone("19907792222");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setTags("[]");
            user.setProfile("爱写代码");
            userList.add(user);
            //userMapper.insert(user);

        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

}