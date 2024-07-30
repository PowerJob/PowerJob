package tech.powerjob.worker.persistence.db;

import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.JavaUtils;
import tech.powerjob.worker.common.constants.StoreStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.h2.Driver;
import tech.powerjob.worker.common.utils.PowerFileUtils;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接管理
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class ConnectionFactory {

    private volatile DataSource dataSource;

    private final String H2_PATH = PowerFileUtils.workspace() + "/h2/" + CommonUtils.genUUID() + "/";
    private final String DISK_JDBC_URL = String.format("jdbc:h2:file:%spowerjob_worker_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", H2_PATH);
    private final String MEMORY_JDBC_URL = String.format("jdbc:h2:mem:%spowerjob_worker_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false", H2_PATH);

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public synchronized void initDatasource(StoreStrategy strategy) {

        // H2 兼容性问题较多，前置输出版本方便排查
        log.info("[PowerDatasource] H2 database version: {}", JavaUtils.determinePackageVersion(Driver.class));

        // 兼容单元测试，否则没办法单独测试 DAO 层了
        strategy = strategy == null ? StoreStrategy.DISK : strategy;

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(Driver.class.getName());
        config.setJdbcUrl(strategy == StoreStrategy.DISK ? DISK_JDBC_URL : MEMORY_JDBC_URL);
        config.setAutoCommit(true);
        // 池中最小空闲连接数量
        config.setMinimumIdle(2);
        // 池中最大连接数量
        config.setMaximumPoolSize(32);
        dataSource = new HikariDataSource(config);

        log.info("[PowerDatasource] init h2 datasource successfully, use url: {}", config.getJdbcUrl());

        // JVM 关闭时删除数据库文件
        try {
            FileUtils.forceDeleteOnExit(new File(H2_PATH));
            log.info("[PowerDatasource] delete worker db file[{}] on JVM exit successfully", H2_PATH);
        }catch (Throwable t) {
            log.warn("[PowerDatasource] delete file on JVM exit failed: {}", H2_PATH, t);
        }
    }

}
