package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import tech.powerjob.official.processors.TestUtils;
import tech.powerjob.official.processors.impl.sql.SimpleSpringSqlProcessor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Echo009
 * @since 2021/3/11
 */
@Slf4j
class SimpleSpringSqlProcessorTest {

    private static SimpleSpringSqlProcessor simpleSpringSqlProcessor;

    @BeforeAll
    static void initSqlProcessor() {

        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase database = builder.setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db_init.sql")
                .build();
        simpleSpringSqlProcessor = new SimpleSpringSqlProcessor(database);
        // do nothing
        simpleSpringSqlProcessor.registerSqlValidator("fakeSqlValidator", (sql) -> true);
        // 排除掉包含 drop 的 SQL
        simpleSpringSqlProcessor.registerSqlValidator("interceptDropValidator", (sql) -> sql.matches("^(?i)((?!drop).)*$"));
        // do nothing
        simpleSpringSqlProcessor.setSqlParser((sql, taskContext) -> sql);
        log.info("init sql processor successfully!");

    }


    @Test
    void testSqlValidator() {
        SimpleSpringSqlProcessor.SqlParams sqlParams = new SimpleSpringSqlProcessor.SqlParams();
        sqlParams.setSql("drop table test_table");
        // 校验不通过
        assertThrows(IllegalArgumentException.class, () -> simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testIncorrectDataSourceName() {
        SimpleSpringSqlProcessor.SqlParams sqlParams = constructSqlParam("create table task_info (a varchar(255), b varchar(255), c varchar(255))");
        sqlParams.setDataSourceName("(๑•̀ㅂ•́)و✧");
        // 数据源名称非法
        assertThrows(IllegalArgumentException.class, () -> simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams))));
    }

    @Test
    void testExecDDL() {
        SimpleSpringSqlProcessor.SqlParams sqlParams = constructSqlParam("create table power_job (a varchar(255), b varchar(255), c varchar(255))");
        ProcessResult processResult = simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams)));
        assertTrue(processResult.isSuccess());
    }

    @Test
    void testExecSQL() {

        SimpleSpringSqlProcessor.SqlParams sqlParams1 = constructSqlParam("insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        ProcessResult processResult1 = simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams1)));
        assertTrue(processResult1.isSuccess());

        assertThrows(JdbcSQLIntegrityConstraintViolationException.class, () -> simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams1))));
        // 第二条会失败回滚
        SimpleSpringSqlProcessor.SqlParams sqlParams2 = constructSqlParam("insert into test_table (id, content) values (1, '?');insert into test_table (id, content) values (0, 'Fight for a better tomorrow')");
        assertThrows(JdbcSQLIntegrityConstraintViolationException.class, () -> simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams2))));
        // 上方回滚，这里就能成功插入
        SimpleSpringSqlProcessor.SqlParams sqlParams3 = constructSqlParam("insert into test_table (id, content) values (1, '?')");
        ProcessResult processResult3 = simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams3)));
        assertTrue(processResult3.isSuccess());

        SimpleSpringSqlProcessor.SqlParams sqlParams4 = constructSqlParam("insert into test_table (id, content) values (2, '?');insert into test_table (id, content) values (3, '?')");
        ProcessResult processResult4 = simpleSpringSqlProcessor.process0(TestUtils.genTaskContext(JSON.toJSONString(sqlParams4)));
        assertTrue(processResult4.isSuccess());

    }

    static SimpleSpringSqlProcessor.SqlParams constructSqlParam(String sql){
        SimpleSpringSqlProcessor.SqlParams sqlParams = new SimpleSpringSqlProcessor.SqlParams();
        sqlParams.setSql(sql);
        return sqlParams;
    }

}
