package com.github.kfcfans.oms.worker.persistence;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.StoreStrategy;
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

    private static final String DISK_JDBC_URL = "jdbc:h2:file:~/oms/h2/oms_worker_db";
    private static final String MEMORY_JDBC_URL = "jdbc:h2:mem:~/oms/h2/oms_worker_db";

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static DataSource getDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        synchronized (ConnectionFactory.class) {
            if (dataSource == null) {

                StoreStrategy strategy = OhMyWorker.getConfig().getStoreStrategy();

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
