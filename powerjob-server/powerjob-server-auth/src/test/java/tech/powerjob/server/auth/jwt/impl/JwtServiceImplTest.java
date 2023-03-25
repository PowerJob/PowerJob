package tech.powerjob.server.auth.jwt.impl;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * test JWT
 *
 * @author tjq
 * @since 2023/3/25
 */
@Slf4j
class JwtServiceImplTest {

    @Test
    void testEncodeAndDecode() {
        Map<String, Object> body = Maps.newHashMap();
        body.put("userId", 277);
        body.put("name", "tjq");

        final String jwtToken = JwtServiceImpl.innerBuild("tjq", 2, body);
        log.info("[JWT] token: {}", jwtToken);
        final Map<String, Object> retMap = JwtServiceImpl.innerParse("tjq", jwtToken);
        log.info("[JWT] parse result: {}", retMap);

        body.forEach((k, v) -> {
            assert v.equals(retMap.get(k));
        });

        // 不匹配情况
        boolean throwExp = false;
        try {
            JwtServiceImpl.innerParse("zqq", jwtToken);
        } catch (Exception e) {
            throwExp = true;
        }
        assert throwExp;
    }
}