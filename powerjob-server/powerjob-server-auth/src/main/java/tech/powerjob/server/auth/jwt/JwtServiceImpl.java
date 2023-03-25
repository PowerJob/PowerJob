package tech.powerjob.server.auth.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import tech.powerjob.server.auth.PowerJobUser;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Server
public class JwtServiceImpl implements JwtService {

    @Resource
    private TokenProvider tokenProvider;

    private int jwtExpireTime;

    @Override
    public String generateToken(PowerJobUser user) {

        JwtBuilder jwtBuilder = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .setSubject("PowerJob")
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * jwtExpireTime))
                .setId(UUID.randomUUID().toString())
                .signWith(null);

        return null;
    }
}
