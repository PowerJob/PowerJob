package tech.powerjob.worker.test;

import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.persistence.DbTaskPersistenceService;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static tech.powerjob.worker.core.tracker.task.heavy.CommonTaskTracker.ROOT_TASK_ID;

/**
 * H2 数据库持久化测试
 *
 * @author tjq
 * @since 2020/3/23
 */
public class PersistenceServiceTest {

    private static final TaskPersistenceService taskPersistenceService = new DbTaskPersistenceService(StoreStrategy.DISK);

    @BeforeAll
    public static void initTable() throws Exception {
        taskPersistenceService.init();

        System.out.println("=============== init data ===============");
        List<TaskDO> taskList = Lists.newLinkedList();
        for (int i = 0; i < 10; i++) {
            TaskDO task = new TaskDO();
            taskList.add(task);

            long instanceId = 10086L + ThreadLocalRandom.current().nextInt(2);
            task.setSubInstanceId(instanceId);
            task.setInstanceId(instanceId);
            task.setTaskId(i + "");
            task.setFailedCnt(0);
            task.setStatus(TaskStatus.WORKER_RECEIVED.getValue());
            task.setTaskName("ROOT_TASK");
            task.setAddress(NetUtils.getLocalHost4Test());
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
            task.setLastReportTime(System.currentTimeMillis());
            task.setResult("");
        }

        taskPersistenceService.batchSave(taskList);
        taskList.forEach(System.out::println);
    }

    @AfterAll
    public static void stop() throws Exception {
        Thread.sleep(60000);
    }


    @Test
    public void testBatchSave(){
        List<TaskDO> taskList = Lists.newLinkedList();
        long instanceId = 10086L + ThreadLocalRandom.current().nextInt(2);
        for (int i = 0; i < 100; i++) {
            TaskDO task = new TaskDO();
            taskList.add(task);
            task.setSubInstanceId(instanceId);
            task.setInstanceId(instanceId);
            task.setTaskId(ROOT_TASK_ID + "." + i);
            task.setFailedCnt(0);
            task.setStatus(TaskStatus.WORKER_RECEIVED.getValue());
            task.setTaskName("ROOT_TASK");
            task.setAddress(NetUtils.getLocalHost4Test());
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
            task.setLastReportTime(System.currentTimeMillis());
            task.setResult("");
        }
        TaskDO firstTask = taskList.get(0);
        taskList.add(firstTask);
        taskPersistenceService.batchSave(taskList);

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
        boolean success = taskPersistenceService.updateLostTasks(10086L, Lists.newArrayList(NetUtils.getLocalHost4Test()), true);
        System.out.println("updateLostTasks: " + success);
    }

    @Test
    public void testGetAllUnFinishedTaskByAddress() throws Exception {
        System.out.println("=============== testGetAllUnFinishedTaskByAddress ===============");
        List<TaskDO> res = taskPersistenceService.getAllUnFinishedTaskByAddress(10086L, NetUtils.getLocalHost4Test());
        System.out.println(res);
    }

}
