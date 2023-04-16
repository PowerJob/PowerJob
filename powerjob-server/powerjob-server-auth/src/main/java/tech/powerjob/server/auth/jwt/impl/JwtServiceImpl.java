package tech.powerjob.server.auth.jwt.impl;

import com.google.common.collect.Maps;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.SecretProvider;

import javax.annotation.Resource;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Resource
    private SecretProvider secretProvider;

    /**
     * JWT 客户端过期时间
     */
    @Value("${oms.auth.security.jwt.expire-seconds:604800}")
    private int jwtExpireTime;

    /**
     * <a href="https://music.163.com/#/song?id=167975">GoodSong</a>
     */
    private static final String BASE_SECURITY =
            "死去元知万事空" +
            "但悲不见九州同" +
            "王师北定中原日" +
            "家祭无忘告乃翁"
            ;

    @Override
    public String build(Map<String, Object> body) {

        final String secret = secretProvider.fetchSecretKey();
        return innerBuild(secret, jwtExpireTime, body);
    }

    static String innerBuild(String secret, int expireSeconds, Map<String, Object> body) {
        JwtBuilder jwtBuilder = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .addClaims(body)
                .setSubject("PowerJob")
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * expireSeconds))
                .setId(UUID.randomUUID().toString())
                .signWith(genSecretKey(secret), SignatureAlgorithm.HS256);
        return jwtBuilder.compact();
    }

    @Override
    public Map<String, Object> parse(String jwt) {
        return innerParse(secretProvider.fetchSecretKey(), jwt);
    }

    static Map<String, Object> innerParse(String secret, String jwtStr) {
        final Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(genSecretKey(secret))
                .build()
                .parseClaimsJws(jwtStr);
        Map<String, Object> ret = Maps.newHashMap();
        ret.putAll(claimsJws.getBody());
        return ret;
    }

    private static Key genSecretKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(BASE_SECURITY.concat(secret));
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
