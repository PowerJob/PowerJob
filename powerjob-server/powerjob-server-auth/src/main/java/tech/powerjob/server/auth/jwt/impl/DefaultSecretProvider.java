package tech.powerjob.server.auth.jwt.impl;

import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tech.powerjob.server.auth.jwt.SecretProvider;
import tech.powerjob.common.utils.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * PowerJob 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Component
public class DefaultSecretProvider implements SecretProvider {

    @Resource
    private Environment environment;

    private static final String PROPERTY_KEY = "spring.datasource.core.jdbc-url";

    private static final String SECRET_PROPERTY = "oms.auth.security.jwt.secret";

    @Override
    public String fetchSecretKey() {
        String secret = environment.getProperty(SECRET_PROPERTY);
        if (StringUtils.isNotBlank(secret)) {
            // Normalize arbitrary UTF-8 secrets for the existing JWT key encoder.
            return Hashing.sha256().hashString(secret, StandardCharsets.UTF_8).toString();
        }

        // An absent or blank optional secret keeps the legacy signing key and existing tokens.
        try {
            String propertyValue = environment.getProperty(PROPERTY_KEY);
            if (StringUtils.isNotEmpty(propertyValue)) {
                String md5 = DigestUtils.md5(propertyValue);
                if (StringUtils.isNotEmpty(md5)) {
                    return md5;
                }
            }
        } catch (Exception ignore) {
        }

        return "ZQQZJ";
    }
}
