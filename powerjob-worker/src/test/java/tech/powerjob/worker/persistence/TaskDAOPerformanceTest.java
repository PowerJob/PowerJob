package tech.powerjob.worker.persistence;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.persistence.db.ConnectionFactory;
import tech.powerjob.worker.persistence.db.SimpleTaskQuery;
import tech.powerjob.worker.persistence.db.TaskDAO;
import tech.powerjob.worker.persistence.db.TaskDAOImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务持久化层 - 性能测试
 *
 * @author tjq
 * @since 2024/2/4
 */
@Slf4j(topic = "PERFORMANCE_TEST_LOGGER")
public class TaskDAOPerformanceTest extends AbstractTaskDAOTest {

    private static final int INSERT_NUM = 100000;

    private static final Long INSTANCE_ID = 10086L;

    @Test
    void testInsert() throws Exception {
        TaskDAO noIndexDao = initTaskDao(false);
        TaskDAO indexDao = initTaskDao(true);

        for (int i = 0; i < 1; i++) {
            testWriteThenRead(noIndexDao, INSERT_NUM, "no-idx-" + i);
            testWriteThenRead(indexDao, INSERT_NUM, "uu-idx-" + i);
        }
    }

    @SneakyThrows
    private void testWriteThenRead(TaskDAO taskDAO, int num, String taskName) {

        String logKey = "testWriteThenRead-" + taskName;
        StopWatch stopWatch = new StopWatch();


        AtomicLong atomicLong = new AtomicLong();

        ForkJoinPool pool = new ForkJoinPool(256);

        CountDownLatch latch = new CountDownLatch(num);

        stopWatch.start("Insert");
        for (int i = 0; i < num; i++) {
            pool.execute(() -> {
                long id = atomicLong.incrementAndGet();
                String taskId = String.format("%s.%d", taskName, id);
                TaskDO taskDO = buildTaskDO(taskId, INSTANCE_ID, TaskStatus.of(ThreadLocalRandom.current().nextInt(1, 7)));
                try {
                    long s = System.currentTimeMillis();
                    taskDAO.save(taskDO);
                    long cost = System.currentTimeMillis() - s;
                    if (cost > 10) {
                        log.warn("[{}] id={} save cost too much: {}", logKey, id, cost);
                    }
                } catch (Exception e) {
                    log.error("[{}] id={} save failed!", logKey, id, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        stopWatch.stop();


        stopWatch.start("READ-getAllTaskResult");
        // 测试读
        List<TaskResult> allTaskResult = taskDAO.getAllTaskResult(INSTANCE_ID, INSTANCE_ID);
        stopWatch.stop();

        // 测试统计
        stopWatch.start("READ-countByStatus");
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(INSTANCE_ID);
        query.setSubInstanceId(INSTANCE_ID);
        query.setQueryContent("status, count(*) as num");
        query.setOtherCondition("GROUP BY status");
        List<Map<String, Object>> countByStatus = taskDAO.simpleQueryPlus(query);
        stopWatch.stop();

        String prettyPrint = stopWatch.prettyPrint();
        System.out.println(logKey + ": " + prettyPrint);
        log.info("[{}] {}", logKey, prettyPrint);

    }

    @SneakyThrows
    private TaskDAO initTaskDao(boolean useIndex) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.initDatasource(StoreStrategy.DISK);
        TaskDAO taskDAO = new TaskDAOImpl(useIndex, connectionFactory);

        taskDAO.initTable();
        return taskDAO;
    }
}
