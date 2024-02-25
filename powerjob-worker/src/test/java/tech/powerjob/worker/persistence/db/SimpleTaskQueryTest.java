package tech.powerjob.worker.persistence.db;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleTaskQueryTest
 *
 * @author tjq
 * @since 2024/2/24
 */
class SimpleTaskQueryTest {

    @Test
    void test() {
        SimpleTaskQuery simpleTaskQuery = new SimpleTaskQuery();
        simpleTaskQuery.setInstanceId(10086L);
        simpleTaskQuery.setTaskIds(Lists.newArrayList("taskId1", "taskId2", "taskId3"));

        String queryCondition = simpleTaskQuery.getQueryCondition();
        System.out.println(queryCondition);

        assertEquals("task_id in ('taskId1', 'taskId2', 'taskId3') and instance_id = 10086", queryCondition);
    }

}