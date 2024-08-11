package tech.powerjob.server.auth.jwt;

import java.util.Map;

/**
 * JWT 服务
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface JwtService {

    String build(Map<String, Object> body, String extraSk);

    ParseResult parse(String jwt, String extraSk);
}
