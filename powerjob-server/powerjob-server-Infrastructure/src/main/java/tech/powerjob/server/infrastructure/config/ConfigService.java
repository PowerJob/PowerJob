package tech.powerjob.server.infrastructure.config;

/**
 * 配置服务
 *
 * @author tjq
 * @since 2024/8/24
 */
public interface ConfigService {

    /**
     * 获取配置
     * @param key 配置名称
     * @param defaultValue 默认值
     * @return 结果
     */
    String fetchConfig(String key, String defaultValue);
}
