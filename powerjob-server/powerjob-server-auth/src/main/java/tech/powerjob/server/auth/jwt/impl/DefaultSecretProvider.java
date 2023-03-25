package tech.powerjob.server.auth.jwt.impl;

import org.springframework.stereotype.Component;
import tech.powerjob.server.auth.jwt.SecretProvider;

/**
 * PowerJob 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Component
public class DefaultSecretProvider implements SecretProvider {
    @Override
    public String fetchSecretKey() {
        return "ZQQ";
    }
}
