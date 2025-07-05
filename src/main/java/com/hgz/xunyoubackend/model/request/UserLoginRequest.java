package com.hgz.xunyoubackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author hgz
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -5828542809121242663L;

    private String userAccount;

    private String userPassword;

}
