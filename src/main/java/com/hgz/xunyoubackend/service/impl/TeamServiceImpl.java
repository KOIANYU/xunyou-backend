package com.hgz.xunyoubackend.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hgz.xunyoubackend.common.ErrorCode;
import com.hgz.xunyoubackend.constant.TeamStatusEnum;
import com.hgz.xunyoubackend.exception.BusinessException;
import com.hgz.xunyoubackend.model.domain.Team;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.model.domain.UserTeam;
import com.hgz.xunyoubackend.model.dto.TeamQuery;
import com.hgz.xunyoubackend.model.request.TeamJoinRequest;
import com.hgz.xunyoubackend.model.request.TeamUpdateRequest;
import com.hgz.xunyoubackend.model.vo.TeamUserVo;
import com.hgz.xunyoubackend.model.vo.UserVO;
import com.hgz.xunyoubackend.service.TeamService;
import com.hgz.xunyoubackend.mapper.TeamMapper;
import com.hgz.xunyoubackend.service.UserService;
import com.hgz.xunyoubackend.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
* @author hgz
* @description 针对表【team(队伍)】的数据库操作Service实现
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService{

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createTeam(Team team, User currentUser) {
        // 1. 参数校验
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 当前用户是否登录
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 3. 校验信息
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }

        String name = team.getName();
        if (StringUtils.isBlank( name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }

        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }

        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (teamStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 如果过期时间 大于 当前时间 说明队伍已经过期了
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }

        // 校验用户最多创建五个队伍
        final long userId = currentUser.getId();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creatorId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }

        // 4. 插入队伍信息到队伍表
        team.setId(null);
        team.setCreatorId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        // 5. 将信息插入 用户队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        return teamId;
    }

    @Override
    public List<TeamUserVo> searchTeams(TeamQuery teamQuery, User currentUser) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            // 根据 描述（同时包含名称和描述） 模糊查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            // 根据 名称 模糊查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            // 根据 描述 模糊查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            // 查询最大人数相等的
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            // 根据创建人来查询
            Long userId = teamQuery.getCreatorId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("creatorId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
            if (teamStatusEnum == null) {
                teamStatusEnum = TeamStatusEnum.PUBLIC;
            }
            queryWrapper.eq("status", teamStatusEnum.getValue());
        }

        // 不展示已过期的队伍 expireTime is not null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVo> teamUserVoList = new ArrayList<>();
        // 关联查询 创建人用户信息
        for (Team team : teamList) {
            Long creatorId = team.getCreatorId();
            if (creatorId == null) {
                continue;
            }
            // 查询用户信息 并且脱敏
            User user = userService.getById(creatorId);
            TeamUserVo teamUserVo = new TeamUserVo();
            BeanUtils.copyProperties(team, teamUserVo);

            UserVO userVO = new UserVO();
            if (user != null) {
                BeanUtils.copyProperties(user, userVO);
            }
            teamUserVo.setCreateUser(userVO);

            teamUserVoList.add(teamUserVo);
        }

        return teamUserVoList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User currentUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        if (oldTeam.getCreatorId() != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (teamStatusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
            }
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        boolean result = this.updateById(updateTeam);
        return result;
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User currentUser) {
        // TODO 用户多次点击可能会出现重复加入队伍 考虑用分布式锁解决
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 查询要加入的队伍信息 进行校验
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从team表中获取队伍信息
        Team team = this.getById(teamId);
        if (team ==  null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 判断队伍是否过期
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已过期");
        }
        // 禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        // 如果是加入加密队伍 校验密码是否正确
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        // 2. 查询该用户加入的队伍数量
        long userId = currentUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        // 在用户队伍关系表中查询队伍数量
        long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个队伍");
        }

        // 不能重复加入已加入的队伍
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
        if (hasUserJoinTeam > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }

        // 3. 只能加入人数未满的队伍 在用户队伍关系表中 查询该队伍的当前人数
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        long teamHasJoinNum = userTeamService.count(userTeamQueryWrapper);
        if (teamHasJoinNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }

        // 增加用户和队伍的关联信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        boolean result = userTeamService.save(userTeam);

        return result;
    }


}




