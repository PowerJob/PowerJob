package tech.powerjob.server.infrastructure.config.impl;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tech.powerjob.server.infrastructure.config.Config;
import tech.powerjob.server.infrastructure.config.ConfigService;
import tech.powerjob.server.infrastructure.config.DynamicServerConfigCrudService;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * ConfigService
 *
 * @author tjq
 * @since 2024/8/24
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private Environment environment;
    @Resource
    private DynamicServerConfigCrudService dynamicServerConfigCrudService;

    @Override
    public String fetchConfig(String key, String defaultValue) {

        Optional<Config> configByDbOpt = dynamicServerConfigCrudService.fetch(key);
        if (configByDbOpt.isPresent()) {
            return configByDbOpt.get().getValue();
        }

        return environment.getProperty(key, defaultValue);
    }
}
