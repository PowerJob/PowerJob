package com.github.kfcfans.oms;

import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * H2 数据库持久化测试
 *
 * @author tjq
 * @since 2020/3/23
 */
public class PersistenceServiceTest {

    private static TaskPersistenceService taskPersistenceService = TaskPersistenceService.INSTANCE;

    @BeforeAll
    public static void initTable() throws Exception {
        taskPersistenceService.init();

        List<TaskDO> taskList = Lists.newLinkedList();
        for (int i = 0; i < 4; i++) {
            TaskDO task = new TaskDO();
            taskList.add(task);

            task.setJobId("1");
            task.setInstanceId("10086" + ThreadLocalRandom.current().nextInt(2));
            task.setTaskId(i + "");
            task.setFailedCnt(0);
            task.setStatus(TaskStatus.RECEIVE_SUCCESS.getValue());
            task.setTaskName("ROOT_TASK");
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
        }

        taskPersistenceService.batchSave(taskList);
        System.out.println("=============== init data ===============");
        taskList.forEach(System.out::println);
    }

    @AfterAll
    public static void stop() throws Exception {
        Thread.sleep(60000);
    }

    @AfterEach
    public void listData() {
        System.out.println("============= listData =============");
        List<TaskDO> result = taskPersistenceService.listAll();
        System.out.println("size: " + result.size());
        result.forEach(System.out::println);
    }


    @Test
    public void testBatchDelete() {

        System.out.println("=============== testBatchDelete ===============");
        boolean delete = taskPersistenceService.batchDelete("100860", Lists.newArrayList("0", "1"));
        System.out.println("delete result:" + delete);
    }


}
