package tech.powerjob.official.processors.impl.sql;

import tech.powerjob.worker.core.processor.TaskContext;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * 简单 Spring SQL 处理器，目前只能用 Spring Bean 的方式加载
 * 直接忽略 SQL 执行的返回值
 *
 * 注意 :
 * 默认情况下没有过滤任何 SQL
 * 建议生产环境一定要使用 {@link SpringDatasourceSqlProcessor#registerSqlValidator} 方法注册至少一个校验器拦截非法 SQL
 *
 * 默认情况下会直接执行参数中的 SQL
 * 可以通过添加  {@link SpringDatasourceSqlProcessor.SqlParser} 来实现定制 SQL 解析逻辑的需求（比如 宏变量替换，参数替换等）
 *
 * @author Echo009
 * @since 2021/3/10
 */
@Slf4j
public class SpringDatasourceSqlProcessor extends AbstractSqlProcessor {
    /**
     * 默认的数据源名称
     */
    private static final String DEFAULT_DATASOURCE_NAME = "default";
    /**
     * name => data source
     */
    private final Map<String, DataSource> dataSourceMap;

    /**
     * 指定默认的数据源
     *
     * @param defaultDataSource 默认数据源
     */
    public SpringDatasourceSqlProcessor(DataSource defaultDataSource) {
        dataSourceMap = Maps.newConcurrentMap();
        registerDataSource(DEFAULT_DATASOURCE_NAME, defaultDataSource);
    }

    @Override
    Connection getConnection(SqlParams sqlParams, TaskContext taskContext) throws SQLException {
        return dataSourceMap.get(sqlParams.getDataSourceName()).getConnection();
    }

    /**
     * 校验参数，如果校验不通过直接抛异常
     *
     * @param sqlParams SQL 参数信息
     */
    @Override
    protected void validateParams(SqlParams sqlParams) {
        // 检查数据源
        if (StringUtils.isEmpty(sqlParams.getDataSourceName())) {
            // use the default data source when current data source name is empty
            sqlParams.setDataSourceName(DEFAULT_DATASOURCE_NAME);
        }
        dataSourceMap.computeIfAbsent(sqlParams.getDataSourceName(), dataSourceName -> {
            throw new IllegalArgumentException("can't find data source with name " + dataSourceName);
        });
    }

    /**
     * 注册数据源
     *
     * @param dataSourceName 数据源名称
     * @param dataSource     数据源
     */
    public void registerDataSource(String dataSourceName, DataSource dataSource) {
        Objects.requireNonNull(dataSourceName, "DataSource name must not be null");
        Objects.requireNonNull(dataSource, "DataSource must not be null");
        dataSourceMap.put(dataSourceName, dataSource);
        log.info("register data source({})' successfully.", dataSourceName);
    }

    /**
     * 移除数据源
     *
     * @param dataSourceName 数据源名称
     */
    public void removeDataSource(String dataSourceName) {
        DataSource remove = dataSourceMap.remove(dataSourceName);
        if (remove != null) {
            log.warn("remove data source({})' successfully.", dataSourceName);
        }
    }
}
