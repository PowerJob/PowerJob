package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.function.Predicate;

/**
 * SQL 处理器，只能用 Spring Bean 的方式加载
 * 注意 : 默认情况下没有过滤任何 SQL
 * 建议生产环境一定要使用 {@link SqlProcessor#registerSqlValidator} 方法注册至少一个校验器拦截非法 SQL
 *
 * 默认情况下会直接执行参数中的 SQL
 * 可以通过添加  {@link SqlProcessor.SqlParser} 来实现定制 SQL 解析逻辑的需求（比如 宏变量替换，参数替换等）
 *
 * @author Echo009
 * @since 2021/3/10
 */
@Slf4j
public class SqlProcessor extends CommonBasicProcessor {
    /**
     * 默认的数据源名称
     */
    private static final String DEFAULT_DATASOURCE_NAME = "default";
    /**
     * 默认超时时间
     */
    private static final int DEFAULT_TIMEOUT = 60;
    /**
     * name => data source
     */
    private final Map<String, DataSource> dataSourceMap;
    /**
     * name => SQL validator
     * 注意 ：
     * - 返回 true 表示验证通过
     * - 返回 false 表示 SQL 非法，将被拒绝执行
     */
    private final Map<String, Predicate<String>> sqlValidatorMap;
    /**
     * 自定义 SQL 解析器
     */
    private SqlParser sqlParser;

    /**
     * 指定默认的数据源
     *
     * @param defaultDataSource 默认数据源
     */
    public SqlProcessor(DataSource defaultDataSource) {
        dataSourceMap = Maps.newConcurrentMap();
        sqlValidatorMap = Maps.newConcurrentMap();
        registerDataSource(DEFAULT_DATASOURCE_NAME, defaultDataSource);
    }


    @Override
    protected ProcessResult process0(TaskContext taskContext) {

        OmsLogger omsLogger = taskContext.getOmsLogger();
        SqlParams sqlParams = JSON.parseObject(CommonUtils.parseParams(taskContext), SqlParams.class);
        // 检查数据源
        if (StringUtils.isEmpty(sqlParams.getDataSourceName())) {
            sqlParams.setDataSourceName(DEFAULT_DATASOURCE_NAME);
            omsLogger.info("current data source name is empty, use the default data source");
        }
        DataSource dataSource = dataSourceMap.computeIfAbsent(sqlParams.getDataSourceName(), dataSourceName -> {
            throw new IllegalArgumentException("can't find data source with name " + dataSourceName);
        });
        StopWatch stopWatch = new StopWatch("SQL Processor");

        // 解析
        stopWatch.start("Parse SQL");
        if (sqlParser != null) {
            sqlParams.setSql(sqlParser.parse(sqlParams.getSql(), taskContext));
        }
        stopWatch.stop();

        // 校验 SQL
        stopWatch.start("Validate SQL");
        validateSql(sqlParams.getSql());
        stopWatch.stop();

        // 执行
        stopWatch.start("Execute SQL");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setSkipResultsProcessing(true);
        jdbcTemplate.setQueryTimeout(sqlParams.getTimeout() == null ? DEFAULT_TIMEOUT : sqlParams.getTimeout());
        omsLogger.info("start to execute sql: {}", sqlParams.getSql());
        jdbcTemplate.execute(sqlParams.getSql());
        stopWatch.stop();
        omsLogger.info(stopWatch.prettyPrint());
        String message = String.format("execute successfully, used time: %s millisecond", stopWatch.getTotalTimeMillis());
        return new ProcessResult(true, message);
    }

    /**
     * 设置 SQL 验证器
     *
     * @param sqlParser SQL 解析器
     */
    public void setSqlParser(SqlParser sqlParser) {
        this.sqlParser = sqlParser;
    }

    /**
     * 注册一个 SQL 验证器
     *
     * @param validatorName 验证器名称
     * @param sqlValidator  验证器
     */
    public void registerSqlValidator(String validatorName, Predicate<String> sqlValidator) {
        sqlValidatorMap.put(validatorName, sqlValidator);
        log.info("[SqlProcessor]register sql validator({})' successfully.", validatorName);
    }


    /**
     * 注册数据源
     *
     * @param dataSourceName 数据源名称
     * @param dataSource     数据源
     */
    public void registerDataSource(String dataSourceName, DataSource dataSource) {
        Assert.notNull(dataSourceName, "DataSource name must not be null");
        Assert.notNull(dataSource, "DataSource must not be null");
        dataSourceMap.put(dataSourceName, dataSource);
        log.info("[SqlProcessor]register data source({})' successfully.", dataSourceName);
    }

    /**
     * 移除数据源
     *
     * @param dataSourceName 数据源名称
     */
    public void removeDataSource(String dataSourceName) {
        DataSource remove = dataSourceMap.remove(dataSourceName);
        if (remove != null) {
            log.warn("[SqlProcessor]remove data source({})' successfully.", dataSourceName);
        }
    }


    /**
     * 校验 SQL 合法性
     */
    private void validateSql(String sql) {
        if (sqlValidatorMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Predicate<String>> entry : sqlValidatorMap.entrySet()) {
            Predicate<String> validator = entry.getValue();
            if (!validator.test(sql)) {
                throw new IllegalArgumentException("illegal sql, can't pass the validation of " + entry.getKey());
            }
        }
    }

    @Data
    public static class SqlParams {
        /**
         * 数据源名称
         */
        private String dataSourceName;
        /**
         * 需要执行的 SQL
         */
        private String sql;
        /**
         * 超时时间
         */
        private Integer timeout;

    }

    @FunctionalInterface
    public interface SqlParser {
        /**
         * 自定义 SQL 解析逻辑
         *
         * @param sql         原始 SQL 语句
         * @param taskContext 任务上下文
         * @return 解析后的 SQL
         */
        String parse(String sql, TaskContext taskContext);
    }

}
