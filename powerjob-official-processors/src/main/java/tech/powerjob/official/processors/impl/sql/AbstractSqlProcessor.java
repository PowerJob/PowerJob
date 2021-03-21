package tech.powerjob.official.processors.impl.sql;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * SQL Processor
 *
 * 处理流程：
 * * * 解析参数 => 校验参数 => 解析 SQL => 校验 SQL => 执行 SQL
 *
 * 可以通过 {@link AbstractSqlProcessor#registerSqlValidator} 方法注册 SQL 校验器拦截非法 SQL
 * 可以通过指定 {@link AbstractSqlProcessor.SqlParser} 来实现定制 SQL 解析逻辑的需求（比如 宏变量替换，参数替换等）
 *
 * @author Echo009
 * @since 2021/3/12
 */
@Slf4j
public abstract class AbstractSqlProcessor extends CommonBasicProcessor {

    /**
     * 默认超时时间
     */
    protected static final int DEFAULT_TIMEOUT = 60;
    /**
     * name => SQL validator
     * 注意 ：
     * - 返回 true 表示验证通过
     * - 返回 false 表示 SQL 非法，将被拒绝执行
     */
    protected final Map<String, Predicate<String>> sqlValidatorMap = Maps.newConcurrentMap();
    /**
     * 自定义 SQL 解析器
     */
    protected SqlParser sqlParser;

    private static final Joiner JOINER = Joiner.on("|").useForNull("-");


    @Override
    public ProcessResult process0(TaskContext taskContext) {

        OmsLogger omsLogger = taskContext.getOmsLogger();
        // 解析参数
        SqlParams sqlParams = extractParams(taskContext);
        omsLogger.info("origin sql params: {}", JSON.toJSON(sqlParams));
        // 校验参数
        validateParams(sqlParams);

        StopWatch stopWatch = new StopWatch(this.getClass().getSimpleName());
        // 解析
        stopWatch.start("Parse SQL");
        if (sqlParser != null) {
            omsLogger.info("before parse sql: {}", sqlParams.getSql());
            String newSQL = sqlParser.parse(sqlParams.getSql(), taskContext);
            sqlParams.setSql(newSQL);
            omsLogger.info("after parse sql: {}", newSQL);
        }
        stopWatch.stop();

        // 校验 SQL
        stopWatch.start("Validate SQL");
        validateSql(sqlParams.getSql(), omsLogger);
        stopWatch.stop();

        // 执行
        stopWatch.start("Execute SQL");
        omsLogger.info("final sql params: {}", JSON.toJSON(sqlParams));
        executeSql(sqlParams, taskContext);
        stopWatch.stop();

        omsLogger.info(stopWatch.prettyPrint());
        String message = String.format("execute successfully, used time: %s millisecond", stopWatch.getTotalTimeMillis());
        return new ProcessResult(true, message);
    }

    abstract Connection getConnection(SqlParams sqlParams, TaskContext taskContext) throws SQLException;

    /**
     * 执行 SQL
     * @param sqlParams SQL processor 参数信息
     * @param ctx 任务上下文
     */
    @SneakyThrows
    private void executeSql(SqlParams sqlParams, TaskContext ctx) {

        OmsLogger omsLogger = ctx.getOmsLogger();

        boolean originAutoCommitFlag ;
        try (Connection connection = getConnection(sqlParams, ctx)) {
            originAutoCommitFlag = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(sqlParams.getTimeout() == null ? DEFAULT_TIMEOUT : sqlParams.getTimeout());
                statement.execute(sqlParams.getSql());

                connection.commit();

                if (sqlParams.showResult) {
                    outputSqlResult(statement, omsLogger);
                }
            } catch (Throwable e) {
                omsLogger.error("execute sql failed, try to rollback", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originAutoCommitFlag);
            }
        }
    }

    private void outputSqlResult(Statement statement, OmsLogger omsLogger) throws SQLException {
        omsLogger.info("====== SQL EXECUTE RESULT ======");

        for (int index = 0; index < Integer.MAX_VALUE; index++) {

            // 某一个结果集
            ResultSet resultSet = statement.getResultSet();
            if (resultSet != null) {
                try (ResultSet rs = resultSet) {
                    int columnCount = rs.getMetaData().getColumnCount();
                    List<String> columnNames = Lists.newLinkedList();
                    //column – the first column is 1, the second is 2, ...
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(rs.getMetaData().getColumnName(i));
                    }
                    omsLogger.info("[Result-{}] [Columns] {}" + System.lineSeparator(), index, JOINER.join(columnNames));
                    int rowIndex = 0;
                    List<Object> row = Lists.newLinkedList();
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        omsLogger.info("[Result-{}] [Row-{}] {}" + System.lineSeparator(), index, rowIndex++, JOINER.join(row));
                    }
                }
            } else {
                int updateCount = statement.getUpdateCount();
                if (updateCount != -1) {
                    omsLogger.info("[Result-{}] update count: {}", index, updateCount);
                }
            }
            if (((!statement.getMoreResults()) && (statement.getUpdateCount() == -1))) {
                break;
            }
        }
        omsLogger.info("====== SQL EXECUTE RESULT ======");
    }

    /**
     * 提取参数信息
     *
     * @param taskContext 任务上下文
     * @return SqlParams
     */
    protected SqlParams extractParams(TaskContext taskContext) {
        return JSON.parseObject(CommonUtils.parseParams(taskContext), SqlParams.class);
    }

    /**
     * 校验参数，如果校验不通过直接抛异常
     *
     * @param sqlParams SQL 参数信息
     */
    protected void validateParams(SqlParams sqlParams) {
        // do nothing
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
        log.info("register sql validator({})' successfully.", validatorName);
    }


    /**
     * 校验 SQL 合法性
     */
    private void validateSql(String sql, OmsLogger omsLogger) {
        if (sqlValidatorMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Predicate<String>> entry : sqlValidatorMap.entrySet()) {
            Predicate<String> validator = entry.getValue();
            if (!validator.test(sql)) {
                omsLogger.error("validate sql by validator[{}] failed, skip to process!", entry.getKey());
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
        /**
         * jdbc url
         * 具体格式可参考 https://www.baeldung.com/java-jdbc-url-format
         */
        private String jdbcUrl;
        /**
         * 是否展示 SQL 执行结果
         */
        private boolean showResult;
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
