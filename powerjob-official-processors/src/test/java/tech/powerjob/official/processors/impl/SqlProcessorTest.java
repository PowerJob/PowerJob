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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Echo009
 * @since 2021/3/11
 */
@Slf4j
class SqlProcessorTest {

    private static SqlProcessor sqlProcessor;

    @BeforeAll
    static void initSqlProcessor() {

        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase database = builder.setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db_init.sql")
                .build();
        sqlProcessor = new SqlProcessor(database);
        // do nothing
        sqlProcessor.registerSqlValidator("fakeSqlValidator", (sql) -> true);
        // 排除掉包含 drop 的 SQL
        sqlProcessor.registerSqlValidator("interceptDropValidator", (sql) -> sql.matches("^(?i)((?!drop).)*$"));
        // do nothing
        sqlProcessor.setSqlParser((sql, taskContext) -> sql);
        log.info("init sql processor successfully!");

    }


    @Test
    void testSqlValidator() {
        SqlProcessor.SqlParams sqlParams = new SqlProcessor.SqlParams();
        sqlParams.setSql("drop table test_table");
        // 校验不通过
        assertThrows(IllegalArgumentException.class, () -> sqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testIncorrectDataSourceName() {
        SqlProcessor.SqlParams sqlParams = constructSqlParam("create table task_info (a varchar(255), b varchar(255), c varchar(255))");
        sqlParams.setDataSourceName("(๑•̀ㅂ•́)و✧");
        // 数据源名称非法
        assertThrows(IllegalArgumentException.class, () -> sqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testExecDDL() {
        SqlProcessor.SqlParams sqlParams = constructSqlParam("create table power_job (a varchar(255), b varchar(255), c varchar(255))");
        ProcessResult processResult = sqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    @Test
    void testExecSQL() {
        SqlProcessor.SqlParams sqlParams = constructSqlParam("insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        ProcessResult processResult = sqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    static SqlProcessor.SqlParams constructSqlParam(String sql){
        SqlProcessor.SqlParams sqlParams = new SqlProcessor.SqlParams();
        sqlParams.setSql(sql);
        return sqlParams;
    }

}
