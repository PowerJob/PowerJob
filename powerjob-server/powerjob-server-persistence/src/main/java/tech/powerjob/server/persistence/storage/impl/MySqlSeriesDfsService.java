package tech.powerjob.server.persistence.storage.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import tech.powerjob.server.extension.dfs.DownloadRequest;
import tech.powerjob.server.extension.dfs.FileLocation;
import tech.powerjob.server.extension.dfs.FileMeta;
import tech.powerjob.server.extension.dfs.StoreRequest;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Optional;

/**
 * MySQL 特性类似的数据库存储
 *
 * @author tjq
 * @since 2023/8/9
 */
@Slf4j
public class MySqlSeriesDfsService extends AbstractDFsService {

    private DataSource dataSource;

    private static final String TYPE_MYSQL = "mysql_series";

    private static final String KEY_DRIVER_NAME = "driver";
    private static final String KEY_URL = "url";
    private static final String KEY_USERNAME = "username";

    private static final String KEY_PASSWORD = "password";

    private static final String CREATE_TABLE_SQL = "CREATE TABLE\n" +
            "IF\n" +
            "\tNOT EXISTS powerjob_files (\n" +
            "\t\t`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "\t\t`gmt_create` DATETIME NOT NULL COMMENT '创建时间',\n" +
            "\t\t`gmt_modified` DATETIME COMMENT '更新时间',\n" +
            "\t\t`name` VARCHAR ( 255 ) NOT NULL COMMENT '文件名称',\n" +
            "\t\t`bucket` VARCHAR ( 255 ) NOT NULL COMMENT '分桶',\n" +
            "\t\t`extra` VARCHAR ( 255 ) NOT NULL COMMENT '其他信息',\n" +
            "\t\t`version` VARCHAR ( 255 ) NOT NULL COMMENT '版本',\n" +
            "\t\t`data` LONGBLOB NOT NULL COMMENT '文件内容',\n" +
            "\tPRIMARY KEY ( id ) \n" +
            "\t);";

    @Override
    public void store(StoreRequest storeRequest) throws IOException {

    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {

    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {
        return Optional.empty();
    }

    @Override
    protected void init(ApplicationContext applicationContext) {

        Environment env = applicationContext.getEnvironment();

        MySQLProperty mySQLProperty = new MySQLProperty()
                .setDriver(fetchProperty(env, TYPE_MYSQL, KEY_DRIVER_NAME))
                .setUrl(fetchProperty(env, TYPE_MYSQL, KEY_URL))
                .setUsername(fetchProperty(env, TYPE_MYSQL, KEY_USERNAME))
                .setPassword(fetchProperty(env, TYPE_MYSQL, KEY_PASSWORD));

        try {
            initDatabase(mySQLProperty);
            initTable(mySQLProperty);
        } catch (Exception e) {
            log.error("[MySqlSeriesDfsService] init datasource failed!", e);
            ExceptionUtils.rethrow(e);
        }
    }

    private void initDatabase(MySQLProperty property) {

        log.info("[MySqlSeriesDfsService] init datasource by config: {}", property);

        HikariConfig config = new HikariConfig();

        config.setDriverClassName(property.driver);
        config.setJdbcUrl(property.url);
        config.setUsername(property.username);
        config.setPassword(property.password);

        config.setAutoCommit(true);
        // 池中最小空闲连接数量
        config.setMinimumIdle(2);
        // 池中最大连接数量
        config.setMaximumPoolSize(32);

        dataSource = new HikariDataSource(config);
    }

    private void initTable(MySQLProperty property) throws Exception {
        dataSource.getConnection().createStatement().execute(CREATE_TABLE_SQL);
    }

    @Override
    public void destroy() throws Exception {
    }

    @Data
    @Accessors(chain = true)
    static class MySQLProperty {
        private String driver;
        private String url;
        private String username;
        private String password;
    }

    public static void main(String[] args) throws Exception {
        MySQLProperty mySQLProperty = new MySQLProperty()
                .setDriver("com.mysql.cj.jdbc.Driver")
                .setUrl("jdbc:mysql://localhost:3306/powerjob-daily?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai")
                .setUsername("root")
                .setPassword("No1Bug2Please3!");
        MySqlSeriesDfsService mySqlSeriesDfsService = new MySqlSeriesDfsService();
        mySqlSeriesDfsService.initDatabase(mySQLProperty);
        mySqlSeriesDfsService.initTable(mySQLProperty);
    }
}
