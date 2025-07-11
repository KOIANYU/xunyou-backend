package com.hgz.xunyoubackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 退出队伍请求
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -975124434240803734L;

    /**
     * 要退出的 队伍id
     */
    private Long teamId;
}
