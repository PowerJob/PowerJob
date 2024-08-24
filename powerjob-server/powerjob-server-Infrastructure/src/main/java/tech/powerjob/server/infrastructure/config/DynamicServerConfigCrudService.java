package tech.powerjob.server.infrastructure.config;

import java.util.List;
import java.util.Optional;

/**
 * 服务端配置 CRUD 服务
 *
 * @author tjq
 * @since 2024/8/24
 */
public interface DynamicServerConfigCrudService {

    /**
     * 保存配置
     * @param config 配置信息
     */
    void save(Config config);

    Optional<Config> fetch(String key);

    /**
     * 删除配置
     * @param key
     */
    void delete(String key);

    /**
     * 列出所有配置
     * @return 配置
     */
    List<Config> list();
}
