package tech.powerjob.server.persistence.storage.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import javax.annotation.Priority;
import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MySQL 特性类似的数据库存储
 * PS1. 大文件上传可能会报 max_allowed_packet 不足，可根据参数放开数据库限制 set global max_allowed_packet = 500*1024*1024
 * PS2. 官方基于 MySQL 测试，其他数据库使用前请自测，敬请谅解！
 * PS3. 数据库并不适合大规模的文件存储，该扩展仅适用于简单业务，大型业务场景请选择其他存储方案（OSS、MongoDB等）
 * ********************* 配置项 *********************
 *  oms.storage.dfs.mysql_series.driver
 *  oms.storage.dfs.mysql_series.url
 *  oms.storage.dfs.mysql_series.username
 *  oms.storage.dfs.mysql_series.password
 *  oms.storage.dfs.mysql_series.auto_create_table
 *  oms.storage.dfs.mysql_series.table_name
 *
 * @author tjq
 * @since 2023/8/9
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE - 2)
@Conditional(MySqlSeriesDfsService.MySqlSeriesCondition.class)
public class MySqlSeriesDfsService extends AbstractDFsService {

    private DataSource dataSource;

    private static final String TYPE_MYSQL = "mysql_series";

    /**
     * 数据库驱动，MYSQL8 为 com.mysql.cj.jdbc.Driver
     */
    private static final String KEY_DRIVER_NAME = "driver";
    /**
     * 数据库地址，比如 jdbc:mysql://localhost:3306/powerjob-daily?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
     */
    private static final String KEY_URL = "url";
    /**
     * 数据库账号，比如 root
     */
    private static final String KEY_USERNAME = "username";
    /**
     * 数据库密码
     */
    private static final String KEY_PASSWORD = "password";
    /**
     * 是否自动建表
     */
    private static final String KEY_AUTO_CREATE_TABLE = "auto_create_table";
    /**
     * 表名
     */
    private static final String KEY_TABLE_NAME = "table_name";

    /* ********************* SQL region ********************* */

    private static final String DEFAULT_TABLE_NAME = "powerjob_files";

    private static final String CREATE_TABLE_SQL = "CREATE TABLE\n" +
            "IF\n" +
            "\tNOT EXISTS %s (\n" +
            "\t\t`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "\t\t`bucket` VARCHAR ( 255 ) NOT NULL COMMENT '分桶',\n" +
            "\t\t`name` VARCHAR ( 255 ) NOT NULL COMMENT '文件名称',\n" +
            "\t\t`version` VARCHAR ( 255 ) NOT NULL COMMENT '版本',\n" +
            "\t\t`meta` VARCHAR ( 255 ) COMMENT '元数据',\n" +
            "\t\t`length` BIGINT NOT NULL COMMENT '长度',\n" +
            "\t\t`status` INT NOT NULL COMMENT '状态',\n" +
            "\t\t`data` LONGBLOB NOT NULL COMMENT '文件内容',\n" +
            "\t\t`extra` VARCHAR ( 255 ) COMMENT '其他信息',\n" +
            "\t\t`gmt_create` DATETIME NOT NULL COMMENT '创建时间',\n" +
            "\t\t`gmt_modified` DATETIME COMMENT '更新时间',\n" +
            "\tPRIMARY KEY ( id ) \n" +
            "\t);";

    private static final String INSERT_SQL = "insert into %s(bucket, name, version, meta, length, status, data, extra, gmt_create, gmt_modified) values (?,?,?,?,?,?,?,?,?,?);";

    private static final String DELETE_SQL = "DELETE FROM %s ";

    private static final String QUERY_FULL_SQL = "select * from %s";

    private static final String QUERY_META_SQL = "select bucket, name, version, meta, length, status, extra, gmt_create, gmt_modified from %s";


    private void deleteByLocation(FileLocation fileLocation) {
        String dSQLPrefix = fullSQL(DELETE_SQL);
        String dSQL = dSQLPrefix.concat(whereSQL(fileLocation));
        executeDelete(dSQL);
    }

    private void executeDelete(String sql) {
        try (Connection con = dataSource.getConnection()) {
            con.createStatement().executeUpdate(sql);
        }  catch (Exception e) {
            log.error("[MySqlSeriesDfsService] executeDelete failed, sql: {}", sql);
        }
    }

    @Override
    public void store(StoreRequest storeRequest) throws IOException {

        Stopwatch sw = Stopwatch.createStarted();
        String insertSQL = fullSQL(INSERT_SQL);

        FileLocation fileLocation = storeRequest.getFileLocation();

        // 覆盖写，写之前先删除
        deleteByLocation(fileLocation);

        Map<String, Object> meta = Maps.newHashMap();
        meta.put("_server_", NetUtils.getLocalHost());
        meta.put("_local_file_path_", storeRequest.getLocalFile().getAbsolutePath());

        Date date = new Date(System.currentTimeMillis());

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pst = con.prepareStatement(insertSQL);

            pst.setString(1, fileLocation.getBucket());
            pst.setString(2, fileLocation.getName());
            pst.setString(3, "mu");
            pst.setString(4, JsonUtils.toJSONString(meta));
            pst.setLong(5, storeRequest.getLocalFile().length());
            pst.setInt(6, SwitchableStatus.ENABLE.getV());
            pst.setBlob(7, new BufferedInputStream(Files.newInputStream(storeRequest.getLocalFile().toPath())));
            pst.setString(8, null);
            pst.setDate(9, date);
            pst.setDate(10, date);

            pst.execute();

            log.info("[MySqlSeriesDfsService] store [{}] successfully, cost: {}", fileLocation, sw);

        } catch (Exception e) {
            log.error("[MySqlSeriesDfsService] store [{}] failed!", fileLocation);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {

        Stopwatch sw = Stopwatch.createStarted();
        String querySQL = fullSQL(QUERY_FULL_SQL);

        FileLocation fileLocation = downloadRequest.getFileLocation();

        FileUtils.forceMkdirParent(downloadRequest.getTarget());

        try (Connection con = dataSource.getConnection()) {

            ResultSet resultSet = con.createStatement().executeQuery(querySQL.concat(whereSQL(fileLocation)));

            boolean exist = resultSet.next();

            if (!exist) {
                log.warn("[MySqlSeriesDfsService] download file[{}] failed due to not exits!", fileLocation);
                return;
            }

            Blob dataBlob = resultSet.getBlob("data");
            FileUtils.copyInputStreamToFile(new BufferedInputStream(dataBlob.getBinaryStream()), downloadRequest.getTarget());

            log.info("[MySqlSeriesDfsService] download [{}] successfully, cost: {}", fileLocation, sw);

        }  catch (Exception e) {
            log.error("[MySqlSeriesDfsService] download file [{}] failed!", fileLocation, e);
            ExceptionUtils.rethrow(e);
        }

    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {

        String querySQL = fullSQL(QUERY_META_SQL);

        try (Connection con = dataSource.getConnection()) {

            ResultSet resultSet = con.createStatement().executeQuery(querySQL.concat(whereSQL(fileLocation)));

            boolean exist = resultSet.next();

            if (!exist) {
                return Optional.empty();
            }

            FileMeta fileMeta = new FileMeta()
                    .setLength(resultSet.getLong("length"))
                    .setLastModifiedTime(resultSet.getDate("gmt_modified"))
                    .setMetaInfo(JsonUtils.parseMap(resultSet.getString("meta")));
            return Optional.of(fileMeta);

        }  catch (Exception e) {
            log.error("[MySqlSeriesDfsService] fetchFileMeta [{}] failed!", fileLocation);
            ExceptionUtils.rethrow(e);
        }

        return Optional.empty();
    }

    @Override
    public void cleanExpiredFiles(String bucket, int days) {

        // 虽然官方提供了服务端删除的能力，依然强烈建议用户直接在数据库层面配置清理事件！！！

        String dSQLPrefix = fullSQL(DELETE_SQL);
        final long targetTs = DateUtils.addDays(new Date(System.currentTimeMillis()), -days).getTime();
        final String targetDeleteTime = CommonUtils.formatTime(targetTs);
        log.info("[MySqlSeriesDfsService] start to cleanExpiredFiles, targetDeleteTime: {}", targetDeleteTime);
        String fSQL = dSQLPrefix.concat(String.format(" where gmt_modified < '%s'", targetDeleteTime));
        log.info("[MySqlSeriesDfsService] cleanExpiredFiles SQL: {}", fSQL);
        executeDelete(fSQL);
    }

    @Override
    protected void init(ApplicationContext applicationContext) {

        Environment env = applicationContext.getEnvironment();

        MySQLProperty mySQLProperty = new MySQLProperty()
                .setDriver(fetchProperty(env, TYPE_MYSQL, KEY_DRIVER_NAME))
                .setUrl(fetchProperty(env, TYPE_MYSQL, KEY_URL))
                .setUsername(fetchProperty(env, TYPE_MYSQL, KEY_USERNAME))
                .setPassword(fetchProperty(env, TYPE_MYSQL, KEY_PASSWORD))
                .setAutoCreateTable(Boolean.TRUE.toString().equalsIgnoreCase(fetchProperty(env, TYPE_MYSQL, KEY_AUTO_CREATE_TABLE)))
                ;

        try {
            initDatabase(mySQLProperty);
            initTable(mySQLProperty);
        } catch (Exception e) {
            log.error("[MySqlSeriesDfsService] init datasource failed!", e);
            ExceptionUtils.rethrow(e);
        }

        log.info("[MySqlSeriesDfsService] initialize successfully, THIS_WILL_BE_THE_STORAGE_LAYER.");
    }

    void initDatabase(MySQLProperty property) {

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

    void initTable(MySQLProperty property) throws Exception {

        if (property.autoCreateTable) {

            String createTableSQL = fullSQL(CREATE_TABLE_SQL);

            log.info("[MySqlSeriesDfsService] use create table SQL: {}", createTableSQL);
            try (Connection connection = dataSource.getConnection()) {
                connection.createStatement().execute(createTableSQL);
                log.info("[MySqlSeriesDfsService] auto create table successfully!");
            }
        }
    }

    private String fullSQL(String sql) {
        return String.format(sql, parseTableName());
    }

    private String parseTableName() {
        // 误删，兼容本地 unit test
        if (applicationContext == null) {
            return DEFAULT_TABLE_NAME;
        }
        String tableName = fetchProperty(applicationContext.getEnvironment(), TYPE_MYSQL, KEY_TABLE_NAME);
        return StringUtils.isEmpty(tableName) ? DEFAULT_TABLE_NAME : tableName;
    }

    private static String whereSQL(FileLocation fileLocation) {
        return String.format(" where bucket='%s' AND name='%s' ", fileLocation.getBucket(), fileLocation.getName());
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

        private boolean autoCreateTable;
    }

    public static class MySqlSeriesCondition extends PropertyAndOneBeanCondition {
        @Override
        protected List<String> anyConfigKey() {
            return Lists.newArrayList("oms.storage.dfs.mysql_series.url");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
