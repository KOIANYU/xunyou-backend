package com.hgz.xunyoubackend.model.request;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求体
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -3483267937648017527L;

    /**
     * 要加入的 队伍id
     */
    private Long teamId;

    /**
     * 如果是加密队伍 需要密码
     */
    private String password;
}
