package tech.powerjob.official.processors.impl.sql;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.MessageFormatter;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.WorkflowContext;
import tech.powerjob.worker.log.OmsLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SqlResultLoggingTest {

    @Test
    void logsOnlyCurrentRowIncludingNullAndUnicode() throws Exception {
        assertEquals(Arrays.asList("[Result-0] [Row-0] 1|甲", "[Result-0] [Row-1] 2|-", "[Result-0] [Row-2] 3|乙"),
                rows(execute("SELECT * FROM items ORDER BY id", true)));
    }

    @Test
    void keepsSingleRowAndColumnLabels() throws Exception {
        List<String> logs = execute("SELECT * FROM items WHERE id=3", true);
        assertEquals(Collections.singletonList("[Result-0] [Row-0] 3|乙"), rows(logs));
        assertTrue(logs.contains("[Result-0] [Columns] ID|LABEL"));
    }

    @Test
    void emptyResultHasColumnsWithoutRows() throws Exception {
        List<String> logs = execute("SELECT * FROM items WHERE id=0", true);
        assertTrue(rows(logs).isEmpty());
        assertTrue(logs.contains("[Result-0] [Columns] ID|LABEL"));
    }

    @Test
    void showResultFalseDoesNotLogRowsOrColumns() throws Exception {
        List<String> logs = execute("SELECT * FROM items ORDER BY id", false);
        assertFalse(logs.stream().anyMatch(line -> line.startsWith("[Result-")));
    }

    @Test
    void updateStillReportsAffectedCount() throws Exception {
        List<String> logs = execute("UPDATE items SET label='changed' WHERE id<3", true);
        assertTrue(rows(logs).isEmpty());
        assertTrue(logs.contains("[Result-0] update count: 2"));
    }

    private List<String> rows(List<String> logs) {
        return logs.stream().filter(line -> line.startsWith("[Result-") && line.contains("[Row-"))
                .collect(Collectors.toList());
    }

    private List<String> execute(String sql, boolean showResult) throws Exception {
        List<String> logs = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE items(id INT PRIMARY KEY, label VARCHAR(30))");
                statement.execute("INSERT INTO items VALUES(1,'甲'),(2,NULL),(3,'乙')");
            }
            AbstractSqlProcessor processor = new AbstractSqlProcessor() {
                @Override Connection getConnection(SqlParams params, TaskContext context) { return connection; }
            };
            AbstractSqlProcessor.SqlParams params = new AbstractSqlProcessor.SqlParams();
            params.setSql(sql);
            params.setShowResult(showResult);
            TaskContext context = new TaskContext();
            context.setWorkflowContext(new WorkflowContext(null, null));
            context.setJobParams(JSON.toJSONString(params));
            context.setOmsLogger(new OmsLogger() {
                @Override public void info(String pattern, Object... args) {
                    logs.add(MessageFormatter.arrayFormat(pattern, args).getMessage().trim());
                }
                @Override public void debug(String pattern, Object... args) { }
                @Override public void warn(String pattern, Object... args) { }
                @Override public void error(String pattern, Object... args) { fail("Unexpected SQL error"); }
            });
            ProcessResult result = processor.process0(context);
            assertTrue(result.isSuccess());
            assertTrue(connection.isClosed(), "Processor must close its connection");
        }
        return logs;
    }
}
