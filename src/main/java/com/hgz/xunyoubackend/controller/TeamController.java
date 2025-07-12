package com.hgz.xunyoubackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hgz.xunyoubackend.common.BaseResponse;
import com.hgz.xunyoubackend.common.ErrorCode;
import com.hgz.xunyoubackend.common.Result;
import com.hgz.xunyoubackend.common.DeleteRequest;
import com.hgz.xunyoubackend.exception.BusinessException;
import com.hgz.xunyoubackend.model.domain.Team;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.model.domain.UserTeam;
import com.hgz.xunyoubackend.model.dto.TeamQuery;
import com.hgz.xunyoubackend.model.request.TeamAddRequest;
import com.hgz.xunyoubackend.model.request.TeamJoinRequest;
import com.hgz.xunyoubackend.model.request.TeamQuitRequest;
import com.hgz.xunyoubackend.model.request.TeamUpdateRequest;
import com.hgz.xunyoubackend.model.vo.TeamUserVo;
import com.hgz.xunyoubackend.service.TeamService;
import com.hgz.xunyoubackend.service.UserService;
import com.hgz.xunyoubackend.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService  userTeamService;

    @PostMapping("/create")
    public BaseResponse<Long> createTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long result = teamService.createTeam(team, currentUser);
        return Result.success(result);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, currentUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return Result.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam long teamId) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return Result.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVo>> searchTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        List<TeamUserVo> teamList = teamService.searchTeams(teamQuery, currentUser);

        List<Long> teamIdList = teamList.stream().map(TeamUserVo::getId).collect(Collectors.toList());
        // 根据所有的 队伍id 和 当前用户id 查询当前用户加入的队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUser.getId());
        queryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        // 获取当前用户加入的所有 队伍id 然后将加入的队伍id与原队伍信息进行匹配 将其hasJoin字段设置为true
        Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teamList.forEach(team -> {
            boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
            team.setHasJoin(hasJoin);
        });

        // 根据队伍id 在 用户队伍关系表中 查询所有用户队伍信息
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList1 = userTeamService.list(userTeamJoinQueryWrapper);
        // 根据查询出的所有用户-队伍关系数据（userTeamList1），按 teamId 分组
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList1.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        // 将每个分组的长度 赋给 team.hasJoinNum
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));

        return Result.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> teamPages = teamService.page(page, queryWrapper);
        return Result.success(teamPages);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, currentUser);
        return Result.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, currentUser);
        return Result.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User currentUser = userService.getCurrentUser(request);
        boolean result = teamService.deleteTeam(id, currentUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return Result.success(true);
    }

    /**
     * 获取用户创建的队伍 队伍表中查询
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/has_created")
    public BaseResponse<List<TeamUserVo>> hasCreatedTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        // 获取队伍创建者id作为查询条件
        teamQuery.setCreatorId(currentUser.getId());
        List<TeamUserVo> teamList = teamService.searchTeams(teamQuery, currentUser);
        // 用户创建的队伍肯定是已经加入的 设置所有的 hasJoin = true
        teamList.forEach(teamUserVo -> teamUserVo.setHasJoin(true));

        return Result.success(teamList);
    }

    /**
     * 获取用户加入的队伍 用户队伍关系表中查询
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/has_joined")
    public BaseResponse<List<TeamUserVo>> hasJoinedTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        // 获取用户加入的所有队伍id
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setTeamIdList(idList);
        List<TeamUserVo> teamList = teamService.searchTeams(teamQuery, currentUser);
        // 获取到的队伍列表是当前用户已经加入的了 设置所有的 hasJoin = true
        teamList.forEach(teamUserVo -> teamUserVo.setHasJoin(true));

        return Result.success(teamList);
    }

}
