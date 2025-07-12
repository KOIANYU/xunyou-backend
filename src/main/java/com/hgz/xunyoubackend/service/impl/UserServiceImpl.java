package com.hgz.xunyoubackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hgz.xunyoubackend.common.ErrorCode;
import com.hgz.xunyoubackend.constant.UserConstant;
import com.hgz.xunyoubackend.exception.BusinessException;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.mapper.UserMapper;
import com.hgz.xunyoubackend.service.UserService;
import com.hgz.xunyoubackend.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
* @author hgz
* @description 针对表【user(用户)】的数据库操作Service实现
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 加密盐
     */
    private static final String SALT = "KOIAN";

    /**
     * 特殊字符校验正则表达式
     */
    private static final String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";


    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        if (userPassword.length() < 8 || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        }
        // 账户不能包含特殊字符
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能包含特殊字符");
        }
        // 判断密码是否和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不一致");
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 2. 密码加密
        String md5Password = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(md5Password);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.EXECUTE_ERROR, "账号重复");
        }

        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request 用户登录状态
     * @return
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        if (userPassword.length() < 8 || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        }
        // 账户不能包含特殊字符
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能包含特殊字符");
        }

        // 2. 密码加密
        String md5Password = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 查询账户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", md5Password);
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.EXECUTE_ERROR, "用户查询失败，账号或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        this.updateById(user);

        // 3. 用户脱敏
        User safeUser = getSafeUser(user);

        // 4. 记录用户登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safeUser);

        return safeUser;
    }

    /**
     * 用户信息脱敏
     *
     * @param user
     * @return
     */
    @Override
    public User getSafeUser(User user) {
        if (user == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setUserAccount(user.getUserAccount());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setGender(user.getGender());
        safeUser.setPhone(user.getPhone());
        safeUser.setEmail(user.getEmail());
        safeUser.setUserStatus(user.getUserStatus());
        safeUser.setCreateTime(user.getCreateTime());
        safeUser.setUserRole(user.getUserRole());
        safeUser.setTags(user.getTags());
        safeUser.setProfile(user.getProfile());
        safeUser.setLastLoginTime(user.getLastLoginTime());
        //safeUser.setPlanetCode(user.getPlanetCode());
        return safeUser;
    }

    /**
     * 查询用户信息
     *
     * @param username
     * @return
     */
    @Override
    public List<User> searchUsers(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank( username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = this.list(queryWrapper);
        return userList.stream().map(user -> getSafeUser(user)).collect(Collectors.toList());
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 先获取所有用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper();
        List<User> userList = this.list(queryWrapper);

        // 根据标签筛选用户信息
        Gson gson = new Gson();
        return userList.stream().filter(user -> {
            if (StringUtils.isBlank(user.getTags())) {
                return false;
            }
            String tagStr = user.getTags();
            Set<String> tempTagNameSet = gson.fromJson(tagStr, new TypeToken<Set<String>>(){}.getType());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafeUser).collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     *
     * @param user
     * @return
     */
    @Override
    public int updateUser(User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = user.getId();
        if (userId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = (User)request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 判断 更新信息的用户 和 登录用户 是否为同一个
        if (userId != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 判断用户是否存在
        User oldUser = this.getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 获取用户登录态
        Object userObject = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObject;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // 需要从数据库中查询用户信息 因为session中的用户信息不一定是最新的
        User user = this.getById(userId);
        return this.getSafeUser(user);
    }

    /**
     * 获取首页推荐用户信息
     *
     * @param pagSize
     * @param pagNum
     * @param request
     * @return
     */
    @Override
    public Page<User> recommendUsers(long pagSize, long pagNum, HttpServletRequest request) {
        // 获取当前用户
        User user = this.getCurrentUser(request);
        long userId = user.getId();

        String redisKey = String.format("xunyou:user:recommend:%s:page%s", userId, pagNum);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 先从缓存中获取用户信息
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return userPage;
        }

        // 缓存中不存在 则从数据库中获取
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = this.page(new Page<>(pagNum, pagSize), queryWrapper);

        // 将数据写入缓存
        // TODO 缓存穿透问题遗留 待解决
        try {
            valueOperations.set(redisKey, userPage, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error: ", e);
        }

        return userPage;
    }

    @Override
    public List<User> matchUsers(long num, User currentUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 只差需要的信息 用户id 和 标签 节省性能
        queryWrapper.select("id", "tags");
        // 过滤 标签 为空的用户
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);

        // 获取当前登录用户的标签
        String tags = currentUser.getTags();
        Gson gson = new Gson();
        // 解析用户标签 将 从数据库获取到的 [\"Java\",\"Python\",\"前端\"] Json字符串 转成 ["Java", "Python", "前端"] 的java对象
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {}.getType());

        // 用户列表的下标 => 相似度 Pair: key-value
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算 所有用户 和 当前用户 的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 过滤 无标签 或者 当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == currentUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {}.getType());

            // 距离算法计算分数
            long distance = AlgorithmUtils.minDistanceByTags(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }

        // 按编辑距离由小到大排序 提取前 num 个用户信息
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());

        // 筛选过后的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        // 上面查询到的用户信息只有 id 和 tags 所以经过筛选后 再查一次获取完整的用户信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafeUser(user))
                .collect(Collectors.groupingBy(User::getId));

        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }

        return finalUserList;
    }

    /**
     * 获取最近7天活跃用户id
     *
     * @return
     */
    @Override
    public List<Long> getActiveUserIdsLast7Days() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 查询最近7天内登录过的用户
        queryWrapper.ge("lastLoginTime", new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
        List<User> users = userMapper.selectList(queryWrapper);
        return users.stream().map(User::getId).collect(Collectors.toList());
    }


    /**
     * 根据标签搜索用户 (通过SQL查询)
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Deprecated
    public List<User> searchUserByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper();
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> userList = this.list(queryWrapper);

        return userList.stream().map(this::getSafeUser).collect(Collectors.toList());
    }

}




