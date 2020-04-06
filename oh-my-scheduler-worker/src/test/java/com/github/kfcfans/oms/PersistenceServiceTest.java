package com.github.kfcfans.oms;

import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.common.utils.NetUtils;
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
        for (int i = 0; i < 10; i++) {
            TaskDO task = new TaskDO();
            taskList.add(task);

            task.setJobId(1L);
            task.setInstanceId(10086L + ThreadLocalRandom.current().nextInt(2));
            task.setTaskId(i + "");
            task.setFailedCnt(0);
            task.setStatus(TaskStatus.WORKER_RECEIVED.getValue());
            task.setTaskName("ROOT_TASK");
            task.setAddress(NetUtils.getLocalHost());
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
    public void testDeleteAllTasks() {

        System.out.println("=============== testBatchDelete ===============");
        boolean delete = taskPersistenceService.deleteAllTasks(100860L);
        System.out.println("delete result:" + delete);
    }

    @Test
    public void testUpdateLostTasks() throws Exception {
        Thread.sleep(1000);
        boolean success = taskPersistenceService.updateLostTasks(Lists.newArrayList(NetUtils.getLocalHost()));
        System.out.println("updateLostTasks: " + success);
    }

}
