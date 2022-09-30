package tech.powerjob.worker.test.function;

import tech.powerjob.worker.test.CommonTest;
import tech.powerjob.worker.test.TestUtils;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.core.tracker.processor.ProcessorTracker;
import tech.powerjob.worker.core.tracker.task.heavy.HeavyTaskTracker;
import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import org.junit.jupiter.api.Test;

/**
 * 空闲测试
 *
 * @author tjq
 * @since 2020/6/17
 */
public class IdleTest extends CommonTest {

    @Test
    public void testProcessorTrackerSendIdleReport() throws Exception {
        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("tech.powerjob.worker.test.processors.TestBasicProcessor");
        ProcessorTracker pt = new ProcessorTracker(req, new WorkerRuntime());
        Thread.sleep(300000);
    }

    @Test
    public void testTaskTrackerProcessorIdle() throws Exception {

        ProcessorTrackerStatusReportReq req = ProcessorTrackerStatusReportReq.buildIdleReport(10086L);
        ServerScheduleJobReq serverScheduleJobReq = TestUtils.genServerScheduleJobReq(ExecuteType.STANDALONE, TimeExpressionType.API);

        HeavyTaskTracker taskTracker = HeavyTaskTracker.create(serverScheduleJobReq, new WorkerRuntime());
        if (taskTracker != null) {
            taskTracker.receiveProcessorTrackerHeartbeat(req);
        }
    }
}
