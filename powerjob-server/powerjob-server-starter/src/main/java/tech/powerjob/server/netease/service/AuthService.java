package tech.powerjob.server.netease.service;

import tech.powerjob.server.netease.po.NeteaseUserInfo;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

/**
 * @author Echo009
 * @since 2021/9/6
 */
public interface AuthService {

    /**
     * 获取 access token
     *
     * @param code code
     * @return access token
     */
    String obtainAccessToken(String code);

    /**
     * 获取用户名
     *
     * @param accessToken access token
     * @return 用户名，英文
     */
    NeteaseUserInfo obtainUserInfo(String accessToken);


    /**
     * 生成 jwt
     *
     * @param userInfo 用户信息
     * @return jwt
     */
    String generateJsonWebToken(UserInfoDO userInfo);

    /**
     * 从 jwt 中解析用户信息
     *
     * @param jwt json web token
     * @return 用户信息
     */
    UserInfoDO parseUserInfo(String jwt);

    /**
     * 返回登录链接
     * @return OpenId 登录链接
     */
    String getLoginUrl();

}
