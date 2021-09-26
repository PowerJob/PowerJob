package com.netease.mail.protal.client.config;

import com.netease.mail.chronos.base.constant.AuthConstant;
import com.netease.mail.chronos.base.po.AuthInfo;
import com.netease.mail.quark.commons.serialization.JacksonUtils;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


/**
 * @author Echo009
 * @since 2021/9/18
 */
public class PortalAuthConfig {


    @Value("${chronos.portal.appName}")
    private String appName;

    @Value("${chronos.portal.appSecrets}")
    private String appSecrets;


    @Bean
    public RequestInterceptor getRequestInterceptor() {
        return template -> {
            AuthInfo authInfo = new AuthInfo();
            authInfo.setAppName(appName);
            authInfo.setAppSecrets(appSecrets);
            template.header(AuthConstant.AUTH_HEADER_NAME,JacksonUtils.toString(authInfo));
        };
    }


}
