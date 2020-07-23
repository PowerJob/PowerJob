package com.github.kfcfans.powerjob.server.persistence.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多重数据源配置
 *
 * @author tjq
 * @since 2020/4/27
 */
@Configuration
public class MultiDatasourceConfig {


    @Primary
    @Bean("omsCoreDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.druid.core")
    public DataSource initOmsCoreDatasource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("omsLocalDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.druid.local")
    public DataSource initOmsLocalDatasource() {
        return DruidDataSourceBuilder.create().build();
    }
}
