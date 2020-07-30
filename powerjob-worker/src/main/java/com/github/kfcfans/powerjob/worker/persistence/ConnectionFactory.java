package com.github.kfcfans.powerjob.worker.persistence;

import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.github.kfcfans.powerjob.worker.common.utils.OmsWorkerFileUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接管理
 *
 * @author tjq
 * @since 2020/3/17
 */
public class ConnectionFactory {

    private static volatile DataSource dataSource;

    private static final String DISK_JDBC_URL = String.format("jdbc:h2:file:%spowerjob_worker_db", OmsWorkerFileUtils.getH2WorkDir());
    private static final String MEMORY_JDBC_URL = String.format("jdbc:h2:mem:%spowerjob_worker_db", OmsWorkerFileUtils.getH2WorkDir());

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static DataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        synchronized (ConnectionFactory.class) {
            if (dataSource == null) {

                // 兼容单元测试，否则没办法单独测试 DAO 层了
                StoreStrategy strategy = OhMyWorker.getConfig() == null ? StoreStrategy.DISK : OhMyWorker.getConfig().getStoreStrategy();

                HikariConfig config = new HikariConfig();
                config.setDriverClassName("org.h2.Driver");
                config.setJdbcUrl(strategy == StoreStrategy.DISK ? DISK_JDBC_URL : MEMORY_JDBC_URL);
                config.setAutoCommit(true);
                // 池中最小空闲连接数量
                config.setMinimumIdle(2);
                // 池中最大连接数量
                config.setMaximumPoolSize(32);
                dataSource = new HikariDataSource(config);
            }
        }
        return dataSource;
    }

}
