package tech.powerjob.client.service.impl;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.DigestUtils;

import java.util.Map;

/**
 * 封装鉴权相关逻辑
 *
 * @author tjq
 * @since 2024/2/21
 */
abstract class AppAuthClusterRequestService extends ClusterRequestService {

    protected AppAuthResult appAuthResult;

    public AppAuthClusterRequestService(ClientConfig config) {
        super(config);
    }


    @Override
    public String request(String path, Object body) {
        // 若不存在 appAuthResult，则首先进行鉴权
        if (appAuthResult == null) {
            refreshAppAuthResult();
        }

        Map<String, String> authHeaders = buildAuthHeader();
        String clusterResponse = clusterHaRequest(path, body, authHeaders);

        // TODO

        return null;
    }

    private Map<String, String> buildAuthHeader() {
        Map<String, String> authHeader = Maps.newHashMap();
        authHeader.put(OpenAPIConstant.REQUEST_HEADER_APP_ID, String.valueOf(appAuthResult.getAppId()));
        authHeader.put(OpenAPIConstant.REQUEST_HEADER_ACCESS_TOKEN, appAuthResult.getToken());
        return authHeader;
    }

    @SneakyThrows
    private void refreshAppAuthResult() {
        AppAuthRequest appAuthRequest = buildAppAuthRequest();
        String authResponse = clusterHaRequest(OpenAPIConstant.AUTH_APP, appAuthRequest, null);
        if (StringUtils.isEmpty(authResponse)) {
            throw new PowerJobException(ErrorCodes.CLIENT_HTTP_REQUEST_FAILED, "EMPTY_RESPONSE");
        }

        this.appAuthResult = JsonUtils.parseObject(authResponse, AppAuthResult.class);
    }

    protected AppAuthRequest buildAppAuthRequest() {
        AppAuthRequest appAuthRequest = new AppAuthRequest();
        appAuthRequest.setAppName(config.getAppName());
        appAuthRequest.setEncryptedPassword(DigestUtils.md5(config.getPassword()));
        return appAuthRequest;
    }
}
