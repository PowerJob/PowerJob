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

    Optional<AppInfoDO> findByAppName(String appName);

    /**
     * 获取 AppInfo（带缓存）
     * @param appId appId
     * @param useCache cache
     * @return App 信息
     */
    Optional<AppInfoDO> findById(Long appId, boolean useCache);

    void deleteById(Long appId);

    /**
     * 保存 App
     * @param appInfo app 信息
     * @return 保存后结果
     */
    AppInfoDO save(AppInfoDO appInfo);

    /**
     *
     * @param appName 验证 APP 账号密码
     * @param password 密码
     * @param encryptType 密码类型
     * @return appId
     */
    Long assertApp(String appName, String password, String encryptType);

    Long assertApp(AppInfoDO appInfo, String password, String encryptType);

    String fetchOriginAppPassword(AppInfoDO appInfo);
}
