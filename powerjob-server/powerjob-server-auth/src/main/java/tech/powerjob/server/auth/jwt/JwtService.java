package tech.powerjob.server.auth.jwt;

import tech.powerjob.server.auth.PowerJobUser;

/**
 * JWT 服务
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface JwtService {

    String generateToken(PowerJobUser user);
}
