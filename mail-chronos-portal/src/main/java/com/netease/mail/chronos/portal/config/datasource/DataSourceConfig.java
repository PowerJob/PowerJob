package com.netease.mail.chronos.portal.config.datasource;

import com.netease.mail.chronos.portal.config.datasource.base.ChronosBaseDatasourceConfig;
import com.netease.mail.chronos.portal.config.datasource.support.ChronosSupportDatasourceConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Configuration
@Import({
        ChronosBaseDatasourceConfig.class,
        ChronosSupportDatasourceConfig.class,
})
@EnableConfigurationProperties
public class DataSourceConfig {






}
