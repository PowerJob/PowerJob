package tech.powerjob.official.processors.impl.sql;

import com.alibaba.fastjson.JSON;
import tech.powerjob.worker.core.processor.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
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
class SpringDatasourceSqlProcessorTest {

    private static SpringDatasourceSqlProcessor springDatasourceSqlProcessor;

    @BeforeAll
    static void initSqlProcessor() {

        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase database = builder.setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db_init.sql")
                .build();
        springDatasourceSqlProcessor = new SpringDatasourceSqlProcessor(database);
        // do nothing
        springDatasourceSqlProcessor.registerSqlValidator("fakeSqlValidator", (sql) -> true);
        // 排除掉包含 drop 的 SQL
        springDatasourceSqlProcessor.registerSqlValidator("interceptDropValidator", (sql) -> sql.matches("^(?i)((?!drop).)*$"));
        // add ';'
        springDatasourceSqlProcessor.setSqlParser((sql, taskContext) -> {
            if (!sql.endsWith(";")) {
                return sql + ";";
            }
            return sql;
        });

        // just invoke clean datasource method
        springDatasourceSqlProcessor.removeDataSource("NULL_DATASOURCE");

        log.info("init sql processor successfully!");

    }


    @Test
    void testSqlValidator() {
        SpringDatasourceSqlProcessor.SqlParams sqlParams = new SpringDatasourceSqlProcessor.SqlParams();
        sqlParams.setSql("drop table test_table");
        // 校验不通过
        assertThrows(IllegalArgumentException.class, () -> springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testIncorrectDataSourceName() {
        SpringDatasourceSqlProcessor.SqlParams sqlParams = constructSqlParam("create table task_info (a varchar(255), b varchar(255), c varchar(255))");
        sqlParams.setDataSourceName("(๑•̀ㅂ•́)و✧");
        // 数据源名称非法
        assertThrows(IllegalArgumentException.class, () -> springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testExecDDL() {
        SpringDatasourceSqlProcessor.SqlParams sqlParams = constructSqlParam("create table power_job (a varchar(255), b varchar(255), c varchar(255))");
        ProcessResult processResult = springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    @Test
    void testExecSQL() {

        SpringDatasourceSqlProcessor.SqlParams sqlParams1 = constructSqlParam("insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        ProcessResult processResult1 = springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams1)));
        assertTrue(processResult1.isSuccess());

        assertThrows(JdbcSQLIntegrityConstraintViolationException.class, () -> springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams1))));
        // 第二条会失败回滚
        SpringDatasourceSqlProcessor.SqlParams sqlParams2 = constructSqlParam("insert into test_table (id, content) values (1, '?');insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        assertThrows(JdbcSQLIntegrityConstraintViolationException.class, () -> springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams2))));
        // 上方回滚，这里就能成功插入
        SpringDatasourceSqlProcessor.SqlParams sqlParams3 = constructSqlParam("insert into test_table (id, content) values (1, '?')");
        ProcessResult processResult3 = springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams3)));
        assertTrue(processResult3.isSuccess());

        SpringDatasourceSqlProcessor.SqlParams sqlParams4 = constructSqlParam("insert into test_table (id, content) values (2, '?');insert into test_table (id, content) values (3, '?')");
        ProcessResult processResult4 = springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams4)));
        assertTrue(processResult4.isSuccess());

    }

    @Test
    public void testQuery() {
        SpringDatasourceSqlProcessor.SqlParams insertParams = constructSqlParam("insert into test_table (id, content) values (1, '?');insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(insertParams)));

        SpringDatasourceSqlProcessor.SqlParams queryParams = constructSqlParam("select * from test_table");
        springDatasourceSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(queryParams)));
    }

    static SpringDatasourceSqlProcessor.SqlParams constructSqlParam(String sql){
        SpringDatasourceSqlProcessor.SqlParams sqlParams = new SpringDatasourceSqlProcessor.SqlParams();
        sqlParams.setSql(sql);
        sqlParams.setShowResult(true);
        return sqlParams;
    }

}
