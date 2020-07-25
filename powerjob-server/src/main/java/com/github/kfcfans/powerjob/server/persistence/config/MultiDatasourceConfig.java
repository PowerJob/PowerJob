package com.github.kfcfans.powerjob.server.persistence.config;

import com.alibaba.druid.pool.DruidDataSource;
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

    private static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String H2_JDBC_URL = "jdbc:h2:file:~/powerjob-server/h2/powerjob_server_db";
    private static final int H2_INITIAL_SIZE = 4;
    private static final int H2_MIN_SIZE = 4;
    private static final int H2_MAX_ACTIVE_SIZE = 10;
    private static final String H2_DATASOURCE_NAME = "localDatasource";

    @Primary
    @Bean("omsCoreDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource initOmsCoreDatasource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("omsLocalDatasource")
    public DataSource initOmsLocalDatasource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(H2_DRIVER_CLASS_NAME);
        ds.setUrl(H2_JDBC_URL);
        ds.setInitialSize(H2_INITIAL_SIZE);
        ds.setMinIdle(H2_MIN_SIZE);
        ds.setMaxActive(H2_MAX_ACTIVE_SIZE);
        ds.setName(H2_DATASOURCE_NAME);
        ds.setTestWhileIdle(false);
        return ds;
    }
}
