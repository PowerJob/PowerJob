package com.github.kfcfans.oms.server.web.request;

import lombok.Data;

/**
 * 创建/修改 UserInfo 请求
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
public class ModifyUserInfoRequest {

    private Long id;

    private String username;
    private String password;

    // 手机号
    private String phone;
    // 邮箱地址
    private String email;
}
