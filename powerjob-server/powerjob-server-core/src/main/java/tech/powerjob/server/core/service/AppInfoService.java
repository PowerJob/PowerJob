package tech.powerjob.server.core.service;

/**
 * AppInfoService
 *
 * @author tjq
 * @since 2023/3/4
 */
public interface AppInfoService {
    Long assertApp(String appName, String password);
}
