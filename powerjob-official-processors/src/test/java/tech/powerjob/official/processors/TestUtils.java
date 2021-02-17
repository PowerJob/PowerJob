package tech.powerjob.official.processors;

import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.log.impl.OmsLocalLogger;
import com.github.kfcfans.powerjob.worker.log.impl.OmsServerLogger;

import java.util.concurrent.ThreadLocalRandom;

/**
 * TestUtils
 *
 * @author tjq
 * @since 2021/1/30
 */
public class TestUtils {

    public static TaskContext genTaskContext(String jobParams) {

        long jobId = ThreadLocalRandom.current().nextLong();

        TaskContext taskContext = new TaskContext();
        taskContext.setJobId(jobId);
        taskContext.setInstanceId(jobId);
        taskContext.setJobParams(jobParams);
        taskContext.setTaskId("0.0");
        taskContext.setTaskName("TEST_TASK");
        taskContext.setOmsLogger(new OmsLocalLogger());

        return taskContext;
    }
}
