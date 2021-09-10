package tech.powerjob.server.netease.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Echo009
 * @since 2021/9/6
 */
@Configuration
@Data
public class AuthConfig {
    /**
     * 前端首页地址
     */
    @Value("${netease.auth.homePage}")
    private String homePage;
    /**
     * 后台的地址
     */
    @Value("${netease.auth.domain}")
    private String domain;
    /**
     * 回调地址
     */
    @Value("${netease.auth.callback}")
    private String callbackUrl ;
    /**
     * 客户端 id
     */
    @Value("${netease.auth.clientId}")
    private String clientId;
    /**
     * 客户端密钥
     */
    @Value("${netease.auth.clientSecret}")
    private String clientSecret;
    /**
     * tokenCookieName
     */
    @Value("${netease.auth.cookieName}")
    private String tokenCookieName;
    /**
     * jwtKey
     */
    @Value("${netease.auth.jwtKey}")
    private String jwtKey;


}
