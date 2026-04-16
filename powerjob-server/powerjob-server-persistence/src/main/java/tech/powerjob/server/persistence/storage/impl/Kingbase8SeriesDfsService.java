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
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;
import tech.powerjob.server.extension.dfs.DFsService;
import tech.powerjob.server.extension.dfs.DownloadRequest;
import tech.powerjob.server.extension.dfs.FileLocation;
import tech.powerjob.server.extension.dfs.FileMeta;
import tech.powerjob.server.extension.dfs.StoreRequest;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import javax.annotation.Priority;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kingbase 存储适配 (支持 MySQL / Oracle / SQLServer / PG 模式)
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE - 3)
@Conditional(Kingbase8SeriesDfsService.KingbaseCondition.class)
public class Kingbase8SeriesDfsService extends AbstractDFsService {

    private static final String TYPE = "kingbase";

    private static final String KEY_DRIVER = "driver";
    private static final String KEY_URL = "url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_AUTO_CREATE_TABLE = "auto_create_table";
    private static final String KEY_TABLE_NAME = "table_name";

    private static final String DEFAULT_DRIVER = "com.kingbase8.Driver";
    private static final String DEFAULT_TABLE_NAME = "oms_dfs_store";

    private static final String INSERT_SQL = "INSERT INTO %s (bucket_name, data_key, data, data_length, meta, gmt_create, gmt_modified) VALUES (?,?,?,?,?,?,?)";
    private static final String DELETE_SQL = "DELETE FROM %s";
    private static final String QUERY_SQL = "SELECT bucket_name, data_key, data, data_length, meta, gmt_create, gmt_modified FROM %s";
    private static final String WHERE_BY_LOCATION = " WHERE bucket_name=? AND data_key=?";

    private HikariDataSource dataSource;
    private String tableName;
    private String detectedMode = "unknown";

    @Override
    public void store(StoreRequest storeRequest) throws IOException {
        FileLocation fileLocation = storeRequest.getFileLocation();
        deleteInternal(fileLocation);

        Stopwatch sw = Stopwatch.createStarted();
        Map<String, Object> meta = Maps.newHashMap();
        meta.put("_server_", serverInfo.getIp());
        meta.put("_local_file_path_", storeRequest.getLocalFile().getAbsolutePath());

        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(fullSQL(INSERT_SQL));
             BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(storeRequest.getLocalFile().toPath()))) {

            ps.setString(1, fileLocation.getBucket());
            ps.setString(2, fileLocation.getName());
            ps.setBinaryStream(3, stream);
            ps.setLong(4, storeRequest.getLocalFile().length());
            ps.setString(5, JsonUtils.toJSONString(meta));
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);

            ps.executeUpdate();

            log.info("[Kingbase8SeriesDfsService] store [{}] successfully, mode: {}, cost: {}", fileLocation, detectedMode, sw);
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] store [{}] failed!", fileLocation, e);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {
        FileLocation fileLocation = downloadRequest.getFileLocation();
        Stopwatch sw = Stopwatch.createStarted();

        FileUtils.forceMkdirParent(downloadRequest.getTarget());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(fullSQL(QUERY_SQL) + WHERE_BY_LOCATION)) {

            setLocationParams(ps, fileLocation, 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("[Kingbase8SeriesDfsService] download [{}] failed, record not found", fileLocation);
                    return;
                }
                FileUtils.copyInputStreamToFile(new BufferedInputStream(rs.getBinaryStream("data")), downloadRequest.getTarget());
                log.info("[Kingbase8SeriesDfsService] download [{}] successfully, cost: {}", fileLocation, sw);
            }
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] download [{}] failed!", fileLocation, e);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(fullSQL(QUERY_SQL) + WHERE_BY_LOCATION)) {

            setLocationParams(ps, fileLocation, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                FileMeta fileMeta = new FileMeta()
                        .setLength(rs.getLong("data_length"))
                        .setLastModifiedTime(rs.getTimestamp("gmt_modified"))
                        .setMetaInfo(JsonUtils.parseMap(rs.getString("meta")));
                return Optional.of(fileMeta);
            }
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] fetchFileMeta [{}] failed!", fileLocation, e);
            ExceptionUtils.rethrow(e);
        }
        return Optional.empty();
    }

    @Override
    public void cleanExpiredFiles(String bucket, int days) {
        final long targetTs = DateUtils.addDays(new Date(System.currentTimeMillis()), -days).getTime();
        Timestamp threshold = new Timestamp(targetTs);
        String sql = fullSQL(DELETE_SQL) + " WHERE bucket_name=? AND gmt_modified < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bucket);
            ps.setTimestamp(2, threshold);
            int effect = ps.executeUpdate();
            log.info("[Kingbase8SeriesDfsService] cleanExpiredFiles bucket={}, days={}, affected={}", bucket, days, effect);
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] cleanExpiredFiles failed, bucket={}, days={}", bucket, days, e);
        }
    }

    private void deleteInternal(FileLocation fileLocation) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(fullSQL(DELETE_SQL) + WHERE_BY_LOCATION)) {
            setLocationParams(ps, fileLocation, 1);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] deleteInternal failed, location: {}", fileLocation, e);
        }
    }

    @Override
    protected void init(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        KingbaseProperty property = new KingbaseProperty()
                .setDriver(getOrDefault(env, KEY_DRIVER, DEFAULT_DRIVER))
                .setUrl(fetchProperty(env, TYPE, KEY_URL))
                .setUsername(fetchProperty(env, TYPE, KEY_USERNAME))
                .setPassword(fetchProperty(env, TYPE, KEY_PASSWORD))
                .setTableName(getOrDefault(env, KEY_TABLE_NAME, DEFAULT_TABLE_NAME))
                .setAutoCreateTable(Boolean.TRUE.toString().equalsIgnoreCase(fetchProperty(env, TYPE, KEY_AUTO_CREATE_TABLE)));

        try {
            initDatasource(property);
            initTable(property);
        } catch (Exception e) {
            log.error("[Kingbase8SeriesDfsService] init datasource/table failed!", e);
            ExceptionUtils.rethrow(e);
        }
    }

    private void initDatasource(KingbaseProperty property) {
        log.info("[Kingbase8SeriesDfsService] init datasource by config: {}", property.safeInfo());

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(property.driver);
        config.setJdbcUrl(property.url);
        config.setUsername(property.username);
        config.setPassword(property.password);
        config.setAutoCommit(true);
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(16);

        dataSource = new HikariDataSource(config);
        tableName = property.tableName;
        detectedMode = KingbaseModeDetector.detectMode(property.url);
        log.info("[Kingbase8SeriesDfsService] detected Kingbase mode: {}", detectedMode);
    }

    private void initTable(KingbaseProperty property) throws Exception {
        if (!property.autoCreateTable) {
            return;
        }
        String ddl = buildCreateTableSql(detectedMode, property.tableName);
        log.info("[Kingbase8SeriesDfsService] use DDL: {}", ddl);
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(ddl);
        }
    }

    private String buildCreateTableSql(String mode, String tableName) {
        switch (mode) {
            case "mysql":
                return "CREATE TABLE IF NOT EXISTS " + tableName + " (\n" +
                        "  id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                        "  bucket_name VARCHAR(255) NOT NULL,\n" +
                        "  data_key VARCHAR(255) NOT NULL,\n" +
                        "  data LONGBLOB NOT NULL,\n" +
                        "  data_length BIGINT NOT NULL,\n" +
                        "  meta LONGTEXT,\n" +
                        "  gmt_create DATETIME,\n" +
                        "  gmt_modified DATETIME,\n" +
                        "  UNIQUE KEY uk_bucket_key (bucket_name, data_key)\n" +
                        ")";
            case "oracle":
                return "BEGIN EXECUTE IMMEDIATE 'CREATE TABLE " + tableName + " (\n" +
                        "  id NUMBER(19) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                        "  bucket_name VARCHAR2(255) NOT NULL,\n" +
                        "  data_key VARCHAR2(255) NOT NULL,\n" +
                        "  data BLOB NOT NULL,\n" +
                        "  data_length NUMBER(19) NOT NULL,\n" +
                        "  meta CLOB,\n" +
                        "  gmt_create TIMESTAMP,\n" +
                        "  gmt_modified TIMESTAMP,\n" +
                        "  CONSTRAINT uk_bucket_key UNIQUE (bucket_name, data_key)\n" +
                        ")'; EXCEPTION WHEN OTHERS THEN NULL; END;";
            case "sqlserver":
                return "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='" + tableName + "')\n" +
                        "CREATE TABLE " + tableName + " (\n" +
                        "  id BIGINT IDENTITY(1,1) PRIMARY KEY,\n" +
                        "  bucket_name VARCHAR(255) NOT NULL,\n" +
                        "  data_key VARCHAR(255) NOT NULL,\n" +
                        "  data VARBINARY(MAX) NOT NULL,\n" +
                        "  data_length BIGINT NOT NULL,\n" +
                        "  meta NVARCHAR(MAX),\n" +
                        "  gmt_create DATETIME2,\n" +
                        "  gmt_modified DATETIME2,\n" +
                        "  CONSTRAINT uk_bucket_key UNIQUE (bucket_name, data_key)\n" +
                        ")";
            default:
                return "CREATE TABLE IF NOT EXISTS " + tableName + " (\n" +
                        "  id BIGSERIAL PRIMARY KEY,\n" +
                        "  bucket_name VARCHAR(255) NOT NULL,\n" +
                        "  data_key VARCHAR(255) NOT NULL,\n" +
                        "  data BYTEA NOT NULL,\n" +
                        "  data_length BIGINT NOT NULL,\n" +
                        "  meta TEXT,\n" +
                        "  gmt_create TIMESTAMP,\n" +
                        "  gmt_modified TIMESTAMP,\n" +
                        "  UNIQUE (bucket_name, data_key)\n" +
                        ")";
        }
    }

    private String fullSQL(String template) {
        return String.format(template, tableName);
    }

    private static void setLocationParams(PreparedStatement ps, FileLocation fileLocation, int startIdx) throws SQLException {
        ps.setString(startIdx, fileLocation.getBucket());
        ps.setString(startIdx + 1, fileLocation.getName());
    }

    private String getOrDefault(Environment env, String key, String defaultValue) {
        String value = fetchProperty(env, TYPE, key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    @Override
    public void destroy() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Data
    @Accessors(chain = true)
    static class KingbaseProperty {
        private String driver;
        private String url;
        private String username;
        private String password;
        private String tableName;
        private boolean autoCreateTable;

        String safeInfo() {
            return "KingbaseProperty{" +
                    "driver='" + driver + '\'' +
                    ", url='" + url + '\'' +
                    ", username='" + username + '\'' +
                    ", tableName='" + tableName + '\'' +
                    ", autoCreateTable=" + autoCreateTable +
                    '}';
        }
    }

    public static class KingbaseCondition extends PropertyAndOneBeanCondition {
        @Override
        protected List<String> anyConfigKey() {
            return Lists.newArrayList("oms.storage.dfs.kingbase.url");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
