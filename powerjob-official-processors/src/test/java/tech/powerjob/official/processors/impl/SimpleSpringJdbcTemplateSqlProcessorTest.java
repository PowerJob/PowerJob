package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import tech.powerjob.official.processors.TestUtils;
import tech.powerjob.official.processors.impl.sql.SimpleSpringJdbcTemplateSqlProcessor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Echo009
 * @since 2021/3/11
 */
@Slf4j
class SimpleSpringJdbcTemplateSqlProcessorTest {

    private static SimpleSpringJdbcTemplateSqlProcessor simpleSpringJdbcTemplateSqlProcessor;

    @BeforeAll
    static void initSqlProcessor() {

        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase database = builder.setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db_init.sql")
                .build();
        simpleSpringJdbcTemplateSqlProcessor = new SimpleSpringJdbcTemplateSqlProcessor(database);
        // do nothing
        simpleSpringJdbcTemplateSqlProcessor.registerSqlValidator("fakeSqlValidator", (sql) -> true);
        // 排除掉包含 drop 的 SQL
        simpleSpringJdbcTemplateSqlProcessor.registerSqlValidator("interceptDropValidator", (sql) -> sql.matches("^(?i)((?!drop).)*$"));
        // do nothing
        simpleSpringJdbcTemplateSqlProcessor.setSqlParser((sql, taskContext) -> sql);
        log.info("init sql processor successfully!");

    }


    @Test
    void testSqlValidator() {
        SimpleSpringJdbcTemplateSqlProcessor.SqlParams sqlParams = new SimpleSpringJdbcTemplateSqlProcessor.SqlParams();
        sqlParams.setSql("drop table test_table");
        // 校验不通过
        assertThrows(IllegalArgumentException.class, () -> simpleSpringJdbcTemplateSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testIncorrectDataSourceName() {
        SimpleSpringJdbcTemplateSqlProcessor.SqlParams sqlParams = constructSqlParam("create table task_info (a varchar(255), b varchar(255), c varchar(255))");
        sqlParams.setDataSourceName("(๑•̀ㅂ•́)و✧");
        // 数据源名称非法
        assertThrows(IllegalArgumentException.class, () -> simpleSpringJdbcTemplateSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testExecDDL() {
        SimpleSpringJdbcTemplateSqlProcessor.SqlParams sqlParams = constructSqlParam("create table power_job (a varchar(255), b varchar(255), c varchar(255))");
        ProcessResult processResult = simpleSpringJdbcTemplateSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    @Test
    void testExecSQL() {
        SimpleSpringJdbcTemplateSqlProcessor.SqlParams sqlParams = constructSqlParam("insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        ProcessResult processResult = simpleSpringJdbcTemplateSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    static SimpleSpringJdbcTemplateSqlProcessor.SqlParams constructSqlParam(String sql){
        SimpleSpringJdbcTemplateSqlProcessor.SqlParams sqlParams = new SimpleSpringJdbcTemplateSqlProcessor.SqlParams();
        sqlParams.setSql(sql);
        return sqlParams;
    }

}
