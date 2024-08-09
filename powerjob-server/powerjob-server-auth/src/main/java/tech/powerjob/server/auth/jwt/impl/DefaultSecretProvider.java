package tech.powerjob.server.auth.jwt.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tech.powerjob.server.auth.jwt.SecretProvider;
import tech.powerjob.common.utils.DigestUtils;

import javax.annotation.Resource;

/**
 * PowerJob 默认实现
 *
 * @author tjq
 * @since 2023/3/20
 */
@Slf4j
@Component
public class DefaultSecretProvider implements SecretProvider {

    @Resource
    private Environment environment;

    private static final String PROPERTY_KEY = "spring.datasource.core.jdbc-url";

    @Override
    public String fetchSecretKey() {

        // 考虑到大部分用户都是开箱即用，此处还是提供一个相对安全的默认实现。JDBC URL 部署时必会改，skey 不固定，更安全
        try {
            String propertyValue = environment.getProperty(PROPERTY_KEY);
            if (StringUtils.isNotEmpty(propertyValue)) {
                String md5 = DigestUtils.md5(propertyValue);

                log.debug("[DefaultSecretProvider] propertyValue: {} ==> md5: {}", propertyValue, md5);

                if (StringUtils.isNotEmpty(md5)) {
                    return md5;
                }
            }
        } catch (Exception ignore) {
        }

        return "ZQQZJ";
    }
}
