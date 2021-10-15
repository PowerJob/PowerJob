package tech.powerjob.server.netease.service.impl;

import cn.hutool.jwt.JWT;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.constants.OpenIdConstants;
import tech.powerjob.server.netease.po.NeteaseUserInfo;
import tech.powerjob.server.netease.service.AuthService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Echo009
 * @since 2021/9/6
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String EXPIRE_TIME = "expireTime";

    private static final String USER_INFO = "userInfo";

    private final AuthConfig authConfig;

    /**
     * https://login.netease.com/download/oidc_docs/flow/token_request.html
     */
    @Override
    public String obtainAccessToken(String code) {

        HashMap<String, String> params = new HashMap<>(16);

        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", authConfig.getCallbackUrl());
        params.put("client_id", authConfig.getClientId());
        params.put("client_secret", authConfig.getClientSecret());

        String rtn = HttpUtils.post(OpenIdConstants.TOKEN_ENDPOINT, params);
        log.info("[auth]obtain access token,code = {},rtn = {}", code, rtn);
        JSONObject jsonObject = JSON.parseObject(rtn);
        return (String) jsonObject.get("access_token");
    }

    /**
     * https://login.netease.com/download/oidc_docs/flow/userinfo_request.html
     */
    @Override
    public NeteaseUserInfo obtainUserInfo(String accessToken) {
        HashMap<String, String> params = new HashMap<>(16);
        params.put("access_token", accessToken);
        String rtn = HttpUtils.get(OpenIdConstants.USER_INFO_ENDPOINT, params);

        log.info("[auth]obtain user name,access token = {},rtn = {}", accessToken, rtn);
        return JSON.parseObject(rtn,NeteaseUserInfo.class);
    }

    @Override
    public String generateJsonWebToken(UserInfoDO userInfo) {

        String userInfoStr = JsonUtils.toJSONString(userInfo);

        // 24 hours
        long tokenPreserveTime = 86400000;
        Long expireTime = System.currentTimeMillis() + tokenPreserveTime;


        return JWT.create()
                .setPayload(USER_INFO, userInfoStr)
                .setPayload(EXPIRE_TIME, expireTime)
                .setKey(authConfig.getJwtKey().getBytes(StandardCharsets.UTF_8))
                .sign();

    }

    @Override
    @SneakyThrows
    public UserInfoDO parseUserInfo(String jwtStr) {
        JWT jwt = JWT.of(jwtStr);
        long expireTime = (Long) jwt.getPayload(EXPIRE_TIME);
        long now = System.currentTimeMillis();
        if (now >= expireTime) {
            return null;
        }
        String userInfoStr = (String) jwt.getPayload(USER_INFO);
        return JsonUtils.parseObject(userInfoStr, new TypeReference<UserInfoDO>() {
        });
    }

    /**
     * https://login.netease.com/download/oidc_docs/flow/authorization_request.html
     */
    @Override
    public String getLoginUrl() {
        HashMap<String, String> params = new HashMap<>(16);
        params.put(OpenIdConstants.RESPONSE_TYPE, "code");
        params.put(OpenIdConstants.SCOPE, "openid fullname nickname");
        params.put(OpenIdConstants.CLIENT_ID, authConfig.getClientId());
        params.put(OpenIdConstants.REDIRECT_URI, authConfig.getCallbackUrl());
        String paramString = HttpUtils.constructParamString(params);
        return OpenIdConstants.LOGIN_BASE_URL + "?" + paramString;
    }


}
