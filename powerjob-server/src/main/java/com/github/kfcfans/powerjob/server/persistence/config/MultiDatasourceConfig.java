package com.github.kfcfans.powerjob.server.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
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

    private static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String H2_JDBC_URL = "jdbc:h2:file:~/powerjob-server/h2/powerjob_server_db";
    private static final int H2_MIN_SIZE = 4;
    private static final int H2_MAX_ACTIVE_SIZE = 10;

    @Primary
    @Bean("omsCoreDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.core")
    public DataSource initOmsCoreDatasource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("omsLocalDatasource")
    public DataSource initOmsLocalDatasource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(H2_DRIVER_CLASS_NAME);
        config.setJdbcUrl(H2_JDBC_URL);
        config.setAutoCommit(true);
        // 池中最小空闲连接数量
        config.setMinimumIdle(H2_MIN_SIZE);
        // 池中最大连接数量
        config.setMaximumPoolSize(H2_MAX_ACTIVE_SIZE);
        return new HikariDataSource(config);
    }
}
