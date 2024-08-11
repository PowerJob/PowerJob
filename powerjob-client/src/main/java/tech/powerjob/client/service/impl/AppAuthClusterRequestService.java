package tech.powerjob.client.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.TypeStore;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.client.service.HttpResponse;
import tech.powerjob.client.service.PowerRequestBody;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.enums.EncryptType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.common.utils.MapUtils;

import java.util.Map;

/**
 * 封装鉴权相关逻辑
 *
 * @author tjq
 * @since 2024/2/21
 */
@Slf4j
abstract class AppAuthClusterRequestService extends ClusterRequestService {

    protected AppAuthResult appAuthResult;

    public AppAuthClusterRequestService(ClientConfig config) {
        super(config);
    }


    @Override
    public String request(String path, PowerRequestBody powerRequestBody) {
        // 若不存在 appAuthResult，则首先进行鉴权
        if (appAuthResult == null) {
            refreshAppAuthResult();
        }

        HttpResponse httpResponse = doRequest(path, powerRequestBody);

        // 如果 auth 成功，则代表请求有效，直接返回
        String authStatus = MapUtils.getString(httpResponse.getHeaders(), OpenAPIConstant.RESPONSE_HEADER_AUTH_STATUS);
        if (Boolean.TRUE.toString().equalsIgnoreCase(authStatus)) {
            return httpResponse.getResponse();
        }

        // 否则请求无效，刷新鉴权后重新请求
        log.warn("[PowerJobClient] auth failed[authStatus: {}], try to refresh the auth info", authStatus);
        refreshAppAuthResult();
        httpResponse = doRequest(path, powerRequestBody);

        // 只要请求不失败，直接返回（如果鉴权失败则返回鉴权错误信息，server 保证 response 永远非空）
        return httpResponse.getResponse();
    }

    private HttpResponse doRequest(String path, PowerRequestBody powerRequestBody) {

        // 添加鉴权信息
        Map<String, String> authHeaders = buildAuthHeader();
        powerRequestBody.addHeaders(authHeaders);

        HttpResponse httpResponse = clusterHaRequest(path, powerRequestBody);

        // 任何请求不成功，都直接报错
        if (!httpResponse.isSuccess()) {
            throw new PowerJobException("REMOTE_SERVER_INNER_EXCEPTION");
        }
        return httpResponse;
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
        HttpResponse httpResponse = clusterHaRequest(OpenAPIConstant.AUTH_APP, PowerRequestBody.newJsonRequestBody(appAuthRequest));
        if (!httpResponse.isSuccess()) {
            throw new PowerJobException("AUTH_APP_EXCEPTION!");
        }
        ResultDTO<AppAuthResult> authResultDTO = JSONObject.parseObject(httpResponse.getResponse(), TypeStore.APP_AUTH_RESULT_TYPE);
        if (!authResultDTO.isSuccess()) {
            throw new PowerJobException("AUTH_FAILED_" + authResultDTO.getMessage());
        }

        log.warn("[PowerJobClient] refresh auth info successfully!");
        this.appAuthResult = authResultDTO.getData();
    }

    protected AppAuthRequest buildAppAuthRequest() {
        AppAuthRequest appAuthRequest = new AppAuthRequest();
        appAuthRequest.setAppName(config.getAppName());
        appAuthRequest.setEncryptedPassword(DigestUtils.md5(config.getPassword()));
        appAuthRequest.setEncryptType(EncryptType.MD5.getCode());
        return appAuthRequest;
    }
}
