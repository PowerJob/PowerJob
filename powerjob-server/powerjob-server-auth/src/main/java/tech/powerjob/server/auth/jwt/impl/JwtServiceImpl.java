package tech.powerjob.server.auth.jwt.impl;

import com.google.common.collect.Maps;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.ParseResult;
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
@Slf4j
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
            "CengMengXiangZhangJianZouTianYa" +
                    "KanYiKanShiJieDeFanHua" +
                    "NianShaoDeXinZongYouXieQingKuang" +
                    "RuJinWoSiHaiWeiJia"
            ;

    @Override
    public String build(Map<String, Object> body, String extraSk) {

        final String secret = fetchSk(extraSk);
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
    public ParseResult parse(String jwt, String extraSk) {
        try {
            Map<String, Object> parseResult = innerParse(fetchSk(extraSk), jwt);
            return new ParseResult().setStatus(ParseResult.Status.SUCCESS).setResult(parseResult);
        } catch (ExpiredJwtException expiredJwtException) {
            return new ParseResult().setStatus(ParseResult.Status.EXPIRED).setMsg(expiredJwtException.getMessage());
        } catch (Exception e) {
            log.warn("[JwtService] parse jwt[{}] with extraSk[{}] failed", jwt, extraSk, e);
            return new ParseResult().setStatus(ParseResult.Status.FAILED).setMsg(ExceptionUtils.getMessage(e));
        }
    }

    private String fetchSk(String extraSk) {
        if (StringUtils.isEmpty(extraSk)) {
            return secretProvider.fetchSecretKey();
        }
        return secretProvider.fetchSecretKey().concat(extraSk);
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
