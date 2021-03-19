package tech.powerjob.server.persistence.config;

import tech.powerjob.server.common.utils.OmsFileUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;

/**
 * 多重数据源配置
 *
 * @author tjq
 * @since 2020/4/27
 */
@Configuration
public class MultiDatasourceConfig {

    private static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String H2_JDBC_URL_PATTERN = "jdbc:h2:file:%spowerjob_server_db";
    private static final int H2_MIN_SIZE = 4;
    private static final int H2_MAX_ACTIVE_SIZE = 10;

    @Primary
    @Bean("omsRemoteDatasource")
    @ConfigurationProperties(prefix = "spring.datasource.core")
    public DataSource initOmsCoreDatasource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("omsLocalDatasource")
    public DataSource initOmsLocalDatasource() {
        String h2Path = OmsFileUtils.genH2WorkPath();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(H2_DRIVER_CLASS_NAME);
        config.setJdbcUrl(String.format(H2_JDBC_URL_PATTERN, h2Path));
        config.setAutoCommit(true);
        // 池中最小空闲连接数量
        config.setMinimumIdle(H2_MIN_SIZE);
        // 池中最大连接数量
        config.setMaximumPoolSize(H2_MAX_ACTIVE_SIZE);

        // JVM 关闭时删除文件
        try {
            FileUtils.forceDeleteOnExit(new File(h2Path));
        }catch (Exception ignore) {
        }
        return new HikariDataSource(config);
    }
}
