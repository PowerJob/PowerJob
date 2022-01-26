package tech.powerjob.server.netease.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.service.impl.AuthServiceImpl;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;


/**
 * @author Echo009
 * @since 2021/9/7
 */
@Slf4j
public class AuthServiceTest {


    @Test
    public void testGenerateJsonWebToken() {
        AuthConfig authConfig = new AuthConfig();
        authConfig.setJwtKey("chronos");
        AuthServiceImpl authService = new AuthServiceImpl(authConfig);

        UserInfoDO userInfoDO = new UserInfoDO();
        userInfoDO.setId(999999L);
        userInfoDO.setUsername("ADMIN");
        userInfoDO.setPassword("*");
        userInfoDO.setEmail("ADMIN@corp.netease.com");
        // 生成 本地环境 用的 永久 token
        String token = authService.generateJsonWebToken(userInfoDO);
        log.info(token);
        Assert.assertNotNull(token);
    }


}