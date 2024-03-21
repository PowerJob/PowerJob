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
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import javax.annotation.Priority;
import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * postgresql 特性类似的数据库存储
 * ********************* 配置项 *********************
 *  oms.storage.dfs.postgresql_series.driver
 *  oms.storage.dfs.postgresql_series.url
 *  oms.storage.dfs.postgresql_series.username
 *  oms.storage.dfs.postgresql_series.password
 *  oms.storage.dfs.postgresql_series.auto_create_table
 *  oms.storage.dfs.postgresql_series.table_name
 *
 * @author jetol
 * @since 2024-1-8
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE - 4)
@Conditional(PostgresqlSeriesDfsService.PostgresqlSeriesCondition.class)
public class PostgresqlSeriesDfsService extends AbstractDFsService {

    private DataSource dataSource;

    private static final String TYPE_POSTGRESQL = "postgresql_series";

    /**
     * 数据库驱动，Postgresql 为 org.postgresql.Driver
     */
    private static final String KEY_DRIVER_NAME = "driver";
    /**
     * 数据库地址，比如 jdbc:postgresql://localhost:3306/powerjob-daily
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

    private static final String POWERJOB_FILES_ID_SEQ = "CREATE SEQUENCE powerjob_files_id_seq\n" +
            "    START WITH 1\n" +
            "    INCREMENT BY 1\n" +
            "    NO MINVALUE\n" +
            "    NO MAXVALUE\n" +
            "    CACHE 1;" ;
    private static final String CREATE_TABLE_SQL = "CREATE TABLE powerjob_files (\n" +
            "  id bigint NOT NULL DEFAULT nextval('powerjob_files_id_seq') PRIMARY KEY,\n" +
            "  bucket varchar(255) NOT NULL,\n" +
            "  name varchar(255) NOT NULL,\n" +
            "  version varchar(255) NOT NULL,\n" +
            "  meta varchar(255) NULL DEFAULT NULL,\n" +
            "  length bigint NOT NULL,\n" +
            "  status int NOT NULL,\n" +
            "  data bytea NOT NULL,\n" +
            "  extra varchar(255) NULL DEFAULT NULL,\n" +
            "  gmt_create timestamp without time zone NOT NULL,\n" +
            "  gmt_modified timestamp without time zone NULL DEFAULT NULL\n" +
            ");";

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
            log.error("[PostgresqlSeriesDfsService] executeDelete failed, sql: {}", sql);
        }
    }

    @Override
    public void store(StoreRequest storeRequest) throws IOException, SQLException {

        Stopwatch sw = Stopwatch.createStarted();
        String insertSQL = fullSQL(INSERT_SQL);

        FileLocation fileLocation = storeRequest.getFileLocation();

        // 覆盖写，写之前先删除
        deleteByLocation(fileLocation);

        Map<String, Object> meta = Maps.newHashMap();
        meta.put("_server_", serverInfo.getIp());
        meta.put("_local_file_path_", storeRequest.getLocalFile().getAbsolutePath());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(storeRequest.getLocalFile().toPath()));

        Date date = new Date(System.currentTimeMillis());

        Connection con =null;
        PreparedStatement pst =null;
        try {
            con = dataSource.getConnection();
            //pg库提示报错：org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
            con.setAutoCommit(false);
            log.info("[PostgresqlSeriesDfsService] set autocommit false.");

            pst = con.prepareStatement(insertSQL);

            pst.setString(1, fileLocation.getBucket());
            pst.setString(2, fileLocation.getName());
            pst.setString(3, "mu");
            pst.setString(4, JsonUtils.toJSONString(meta));
            pst.setLong(5, storeRequest.getLocalFile().length());
            pst.setInt(6, SwitchableStatus.ENABLE.getV());
            //PreparedStatement类并没有提供setBlob方法来直接设置BYTEA类型字段，因为PostgreSQL不支持JDBC中的java.sql.Blob接口
//            pst.setBlob(7, bufferedInputStream);org.postgresql.util.PSQLException: ERROR: column "data" is of type bytea but expression is of type bigint
            pst.setBytes(7, bufferedInputStreamToByteArray(bufferedInputStream));
            pst.setString(8, null);
            pst.setDate(9, date);
            pst.setDate(10, date);

            pst.execute();
            con.commit();
            log.info("[PostgresqlSeriesDfsService] store [{}] successfully, cost: {}", fileLocation, sw);

        } catch (Exception e) {
            if(con != null){
                con.rollback();
            }
            log.error("[PostgresqlSeriesDfsService] store [{}] failed!", fileLocation);
            ExceptionUtils.rethrow(e);
        }finally {
            if(con != null){
                //设置回来，恢复自动提交模式
                con.setAutoCommit(true);
                log.info("[PostgresqlSeriesDfsService] set autocommit true.");
                con.close();
            }
            if(null != pst){
                pst.close();
            }
            bufferedInputStream.close();
        }
    }

    /**
     * 上面已经有异常处理，这里直接往上抛
     * @param bis
     * @return
     * @throws IOException
     */
    public static byte[] bufferedInputStreamToByteArray(BufferedInputStream bis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(null == bis ){
            return null;
        }
        // 创建缓冲区
        byte[] buffer = new byte[1024];
        int read;
        // 读取流中的数据并写入到ByteArrayOutputStream
        while ((read = bis.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        // 关闭输入流
        bis.close();
        // 转换为字节数组并返回
        return baos.toByteArray();
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
                log.warn("[PostgresqlSeriesDfsService] download file[{}] failed due to not exits!", fileLocation);
                return;
            }

            byte[] data = resultSet.getBytes("data");
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(data), downloadRequest.getTarget());

            log.info("[PostgresqlSeriesDfsService] download [{}] successfully, cost: {}", fileLocation, sw);

        }  catch (Exception e) {
            log.error("[PostgresqlSeriesDfsService] download file [{}] failed!", fileLocation, e);
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
            log.error("[PostgresqlSeriesDfsService] fetchFileMeta [{}] failed!", fileLocation);
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
        log.info("[PostgresqlSeriesDfsService] start to cleanExpiredFiles, targetDeleteTime: {}", targetDeleteTime);
        String fSQL = dSQLPrefix.concat(String.format(" where gmt_modified < '%s'", targetDeleteTime));
        log.info("[PostgresqlSeriesDfsService] cleanExpiredFiles SQL: {}", fSQL);
        executeDelete(fSQL);
    }

    @Override
    protected void init(ApplicationContext applicationContext) {

        Environment env = applicationContext.getEnvironment();

        PostgresqlProperty postgresqlProperty = new PostgresqlProperty()
                .setDriver(fetchProperty(env, TYPE_POSTGRESQL, KEY_DRIVER_NAME))
                .setUrl(fetchProperty(env, TYPE_POSTGRESQL, KEY_URL))
                .setUsername(fetchProperty(env, TYPE_POSTGRESQL, KEY_USERNAME))
                .setPassword(fetchProperty(env, TYPE_POSTGRESQL, KEY_PASSWORD))
                .setAutoCreateTable(Boolean.TRUE.toString().equalsIgnoreCase(fetchProperty(env, TYPE_POSTGRESQL, KEY_AUTO_CREATE_TABLE)))
                ;

        try {
            initDatabase(postgresqlProperty);
            initTable(postgresqlProperty);
        } catch (Exception e) {
            log.error("[PostgresqlSeriesDfsService] init datasource failed!", e);
            ExceptionUtils.rethrow(e);
        }

        log.info("[PostgresqlSeriesDfsService] initialize successfully, THIS_WILL_BE_THE_STORAGE_LAYER.");
    }

    void initDatabase(PostgresqlProperty property) {

        log.info("[PostgresqlSeriesDfsService] init datasource by config: {}", property);

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

    void initTable(PostgresqlProperty property) throws Exception {

        if (property.autoCreateTable) {

            String powerjobFilesIdSeq = fullSQL(POWERJOB_FILES_ID_SEQ);
            String createTableSQL = fullSQL(CREATE_TABLE_SQL);

            log.info("[PostgresqlSeriesDfsService] use create table SQL: {}", createTableSQL);
            try (Connection connection = dataSource.getConnection()) {
                connection.createStatement().execute(powerjobFilesIdSeq);
                connection.createStatement().execute(createTableSQL);
                log.info("[PostgresqlSeriesDfsService] auto create table successfully!");
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
        String tableName = fetchProperty(applicationContext.getEnvironment(), TYPE_POSTGRESQL, KEY_TABLE_NAME);
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
    static class PostgresqlProperty {
        private String driver;
        private String url;
        private String username;
        private String password;

        private boolean autoCreateTable;
    }

    public static class PostgresqlSeriesCondition extends PropertyAndOneBeanCondition {
        @Override
        protected List<String> anyConfigKey() {
            return Lists.newArrayList("oms.storage.dfs.postgresql_series.url");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
