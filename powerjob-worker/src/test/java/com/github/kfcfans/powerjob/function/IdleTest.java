package com.github.kfcfans.powerjob.function;

import com.github.kfcfans.powerjob.CommonTest;
import com.github.kfcfans.powerjob.TestUtils;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.ServerScheduleJobReq;
import com.github.kfcfans.powerjob.worker.common.RuntimeMeta;
import com.github.kfcfans.powerjob.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.powerjob.worker.core.tracker.task.TaskTracker;
import com.github.kfcfans.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.github.kfcfans.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
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
        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("com.github.kfcfans.powerjob.processors.TestBasicProcessor");
        ProcessorTracker pt = new ProcessorTracker(req, new RuntimeMeta());
        Thread.sleep(300000);
    }

    @Test
    public void testTaskTrackerProcessorIdle() throws Exception {

        ProcessorTrackerStatusReportReq req = ProcessorTrackerStatusReportReq.buildIdleReport(10086L);
        ServerScheduleJobReq serverScheduleJobReq = TestUtils.genServerScheduleJobReq(ExecuteType.STANDALONE, TimeExpressionType.API);

        TaskTracker taskTracker = TaskTracker.create(serverScheduleJobReq, new RuntimeMeta());
        if (taskTracker != null) {
            taskTracker.receiveProcessorTrackerHeartbeat(req);
        }
    }
}
