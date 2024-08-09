package tech.powerjob.server.core.service;

import tech.powerjob.server.persistence.remote.model.AppInfoDO;

import java.util.Optional;

/**
 * AppInfoService
 *
 * @author tjq
 * @since 2023/3/4
 */
public interface AppInfoService {

    /**
     * 验证 APP 账号密码
     * @param appName 账号
     * @param password 原文密码
     * @return AppId
     */
    Long assertApp(String appName, String password);

    Optional<AppInfoDO> findByAppName(String appName);

    /**
     * 获取 AppInfo（带缓存）
     * @param appId appId
     * @return App 信息
     */
    Optional<AppInfoDO> findByIdWithCache(Long appId);
}
