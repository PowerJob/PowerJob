package tech.powerjob.official.processors.impl.sql;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;

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


    @Override
    public ProcessResult process0(TaskContext taskContext) {

        OmsLogger omsLogger = taskContext.getOmsLogger();
        // 解析参数
        SqlParams sqlParams = extractParams(taskContext);
        omsLogger.info("[AbstractSqlProcessor-{}]origin sql params: {}", taskContext.getInstanceId(), JSON.toJSON(sqlParams));
        // 校验参数
        validateParams(sqlParams);

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
        omsLogger.info("[AbstractSqlProcessor-{}]final sql params: {}", taskContext.getInstanceId(), JSON.toJSON(sqlParams));
        executeSql(sqlParams, taskContext);
        stopWatch.stop();

        omsLogger.info(stopWatch.prettyPrint());
        String message = String.format("execute successfully, used time: %s millisecond", stopWatch.getTotalTimeMillis());
        return new ProcessResult(true, message);
    }

    /**
     * 执行 SQL
     *
     * @param sqlParams   SQL processor 参数信息
     * @param taskContext 任务上下文
     */
    abstract void executeSql(SqlParams sqlParams, TaskContext taskContext);

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
        /**
         * jdbc url
         * 具体格式可参考 https://www.baeldung.com/java-jdbc-url-format
         */
        private String jdbcUrl;

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
