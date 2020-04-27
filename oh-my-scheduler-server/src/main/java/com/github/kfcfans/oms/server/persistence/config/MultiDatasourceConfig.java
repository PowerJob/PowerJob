package com.github.kfcfans.oms.server.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
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

    private static final String H2_JDBC_URL = "jdbc:h2:file:~/oms/h2/oms_server_db";

    @Primary
    @Bean("omsCoreDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.core")
    public DataSource initOmsCoreDatasource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("omsLocalDatasource")
    public DataSource initOmsLocalDatasource() {

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(H2_JDBC_URL);
        config.setAutoCommit(true);
        // 池中最小空闲连接数量
        config.setMinimumIdle(4);
        // 池中最大连接数量
        config.setMaximumPoolSize(32);
        return new HikariDataSource(config);
    }
}
