package tech.powerjob.server.openapi.security;

import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;

import javax.servlet.http.HttpServletRequest;

/**
 * OPENAPI 安全服务
 *
 * @author tjq
 * @since 2024/2/19
 */
public interface OpenApiSecurityService {

    /**
     * APP 纬度请求的鉴权 & 验证
     * @param appAuthRequest 请求参数
     * @return token
     */
    AppAuthResult authAppByParam(AppAuthRequest appAuthRequest);

    /**
     * APP 纬度请求的鉴权 & 验证
     * @param httpServletRequest http 原始请求
     */
    void authAppByToken(HttpServletRequest httpServletRequest);
}
