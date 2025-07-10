package com.hgz.xunyoubackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hgz.xunyoubackend.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hgz.xunyoubackend.model.domain.User;
import com.hgz.xunyoubackend.model.dto.TeamQuery;
import com.hgz.xunyoubackend.model.request.TeamUpdateRequest;
import com.hgz.xunyoubackend.model.vo.TeamUserVo;

import java.util.List;

/**
* @author hgz
* @description 针对表【team(队伍)】的数据库操作Service
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @return
     */
    long createTeam(Team team, User currentUser);

    /**
     * 搜索队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVo> searchTeams(TeamQuery teamQuery, User currentUser);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User currentUser);
}
