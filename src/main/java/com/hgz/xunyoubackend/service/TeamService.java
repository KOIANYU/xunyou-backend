package com.hgz.xunyoubackend.service;

import com.hgz.xunyoubackend.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hgz.xunyoubackend.model.domain.User;

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
}
