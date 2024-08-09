package tech.powerjob.client.service.impl;

import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.common.utils.DigestUtils;

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

    protected void refreshAuthInfo() {
        AppAuthRequest appAuthRequest = new AppAuthRequest();
        appAuthRequest.setAppName(config.getAppName());
        appAuthRequest.setEncryptedPassword(DigestUtils.md5(config.getPassword()));

        try {

        } catch (Exception e) {

        }
    }
}
