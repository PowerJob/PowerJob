package com.netease.mail.chronos.executor.config.datasource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Configuration
@Import({
        ChronosSupportDatasourceConfig.class,
})
@EnableConfigurationProperties
public class DataSourceConfig {






}
