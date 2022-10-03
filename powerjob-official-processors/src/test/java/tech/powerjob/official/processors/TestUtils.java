package tech.powerjob.official.processors;

import tech.powerjob.common.model.LogConfig;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.WorkflowContext;
import tech.powerjob.worker.log.impl.OmsLocalLogger;

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
        taskContext.setOmsLogger(new OmsLocalLogger(new LogConfig()));
        taskContext.setWorkflowContext(new WorkflowContext(null, null));
        return taskContext;
    }
}
