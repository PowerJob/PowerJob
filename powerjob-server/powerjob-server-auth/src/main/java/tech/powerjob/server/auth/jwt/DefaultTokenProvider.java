package tech.powerjob.server.auth.jwt;

import org.springframework.stereotype.Component;

/**
 * PowerJob 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Component
public class DefaultTokenProvider implements TokenProvider {
    @Override
    public String fetchToken() {
        return "ZQQ";
    }
}
