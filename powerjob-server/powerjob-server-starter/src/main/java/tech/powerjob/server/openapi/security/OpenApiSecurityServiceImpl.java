package tech.powerjob.server.openapi.security;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.common.utils.HttpServletUtils;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OpenApiSecurityService
 *
 * @author tjq
 * @since 2024/2/19
 */
@Slf4j
@Service
public class OpenApiSecurityServiceImpl implements OpenApiSecurityService {

    @Resource
    private JwtService jwtService;
    @Resource
    private AppInfoService appInfoService;

    private static final String JWT_KEY_APP_ID = "appId";
    private static final String JWT_KEY_APP_PASSWORD = "password";

    @Override
    public void authAppByToken(HttpServletRequest httpServletRequest) {

        String token = HttpServletUtils.fetchFromHeader(OpenAPIConstant.HEADER_ACCESS_TOKEN, httpServletRequest);
        String appIdFromHeader = HttpServletUtils.fetchFromHeader(OpenAPIConstant.HEADER_APP_ID, httpServletRequest);

        if (StringUtils.isEmpty(appIdFromHeader)) {
            throw new PowerJobException(ErrorCodes.INVALID_REQUEST, "lack_of_appId_in_header");
        }

        if (StringUtils.isEmpty(token)) {
            throw new PowerJobException(ErrorCodes.OPEN_API_AUTH_FAILED, "token_is_empty");
        }

        Map<String, Object> jwtResult = jwtService.parse(token, null);

        Long appIdFromJwt = MapUtils.getLong(jwtResult, JWT_KEY_APP_ID);
        String passwordFromJwt = MapUtils.getString(jwtResult, JWT_KEY_APP_PASSWORD);

        // 校验 appId 一致性
        if (!StringUtils.equals(appIdFromHeader, String.valueOf(appIdFromJwt))) {
            throw new PowerJobException(ErrorCodes.INVALID_REQUEST, "Inconsistent_appId_from_token_and_header");
        }

        // 此处不考虑改密码后的缓存时间，毕竟只要改了密码，一定会报错。换言之 OpenAPI 模式下，密码不可更改
        Optional<AppInfoDO> appInfoOpt = appInfoService.findByIdWithCache(appIdFromJwt);
        if (!appInfoOpt.isPresent()) {
            throw new PowerJobException(ErrorCodes.INVALID_APP, "can_not_find_app");
        }

        String dbOriginPassword = appInfoOpt.get().getPassword();
        if (!StringUtils.equals(passwordFromJwt, DigestUtils.md5(dbOriginPassword))) {
            throw new PowerJobException(ErrorCodes.OPEN_API_PASSWORD_ERROR, "password_compare_failed");
        }
    }


    @Override
    public AppAuthResult authAppByParam(AppAuthRequest appAuthRequest) {

        String appName = appAuthRequest.getAppName();
        String encryptedPassword = appAuthRequest.getEncryptedPassword();

        Optional<AppInfoDO> appInfoOpt = appInfoService.findByAppName(appName);
        if (!appInfoOpt.isPresent()) {
            throw new PowerJobAuthException(ErrorCodes.INVALID_APP);
        }

        AppInfoDO appInfo = appInfoOpt.get();

        // 密码验证失败
        if (!Objects.equals(DigestUtils.md5(appInfo.getPassword()), encryptedPassword)) {
            throw new PowerJobAuthException(ErrorCodes.OPEN_API_PASSWORD_ERROR);
        }

        Map<String, Object> jwtBody = Maps.newHashMap();
        jwtBody.put(JWT_KEY_APP_ID, appInfo.getId());
        jwtBody.put(JWT_KEY_APP_PASSWORD, encryptedPassword);

        AppAuthResult appAuthResult = new AppAuthResult();

        appAuthResult.setAppId(appInfo.getId());
        appAuthResult.setToken(jwtService.build(jwtBody, null));

        return appAuthResult;
    }
}
