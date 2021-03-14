package tech.powerjob.official.processors.impl.sql;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.official.processors.util.CommonUtils;
import tech.powerjob.official.processors.util.SecurityUtils;
import tech.powerjob.worker.core.processor.TaskContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * execute sql by dynamic datasource, which is very dangerous but can save your life at the critical moment
 *
 * @author tjq
 * @since 2021/3/14
 */
public class DynamicDatasourceSqlProcessor extends AbstractSqlProcessor {

    @Override
    protected void validateParams(SqlParams sqlParams) {
        if (StringUtils.isEmpty(sqlParams.getJdbcUrl())) {
            throw new IllegalArgumentException("jdbcUrl can't be empty in DynamicDatasourceSqlProcessor!");
        }
    }

    @Override
    Connection getConnection(SqlParams sqlParams, TaskContext taskContext) throws SQLException {

        JSONObject params = JSONObject.parseObject(CommonUtils.parseParams(taskContext));
        Properties properties = new Properties();

        // normally at least a "user" and "password" property should be included
        params.forEach((k, v) -> properties.setProperty(k, String.valueOf(v)));

        return DriverManager.getConnection(sqlParams.getJdbcUrl(), properties);
    }

    @Override
    protected String getSecurityDKey() {
        return SecurityUtils.ENABLE_DYNAMIC_SQL_PROCESSOR;
    }
}
