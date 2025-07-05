package com.hgz.xunyoubackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author hgz
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -2913075033613524213L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;


}
