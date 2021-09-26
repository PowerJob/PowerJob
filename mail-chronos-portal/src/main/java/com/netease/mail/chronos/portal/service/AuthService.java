package com.netease.mail.chronos.portal.service;

/**
 * @author Echo009
 * @since 2021/9/26
 */
public interface AuthService {

    /**
     * 校验权限
     *
     * @param authStr 认证信息
     * @return 是否有权限
     */
    boolean checkPermission(String authStr);


}
