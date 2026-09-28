package tech.powerjob.worker.core.tracker.task.light;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.core.executor.ExecutorManager;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.extension.processor.ProcessorBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LightTaskTrackerWorkflowContextTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(LightTaskTracker.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() { logs.start(); logger.addAppender(logs); }

    @AfterEach
    void stopCapturingLogs() { logger.detachAppender(logs); logs.stop(); }

    @Test
    void emptyWorkflowContextDoesNotWarn() throws Exception {
        assertEquals(Collections.emptyMap(), execute(8192, true, Collections.emptyMap()).getAppendedWfContext());
        assertEquals(0, lengthWarnings());
    }

    @Test
    void workflowContextBelowDefaultLimitIsReported() throws Exception {
        Map<String, String> data = sizedContext(8191);
        assertEquals(data, execute(8192, true, data).getAppendedWfContext());
        assertEquals(0, lengthWarnings());
    }

    @Test
    void workflowContextAtDefaultLimitIsReported() throws Exception {
        Map<String, String> data = sizedContext(8192);
        assertEquals(data, execute(8192, true, data).getAppendedWfContext());
        assertEquals(0, lengthWarnings());
    }

    @Test
    void workflowContextAboveDefaultLimitIsDiscardedWithWarning() throws Exception {
        assertEquals(Collections.emptyMap(), execute(8192, true, sizedContext(8193)).getAppendedWfContext());
        assertEquals(1, lengthWarnings());
    }

    @Test
    void configuredLimitCountsSerializedUnicodeAndEscapesLikeHeavyTasks() throws Exception {
        // {"v":"中😀\"\n"}: 15 UTF-16 code units including the two JSON escapes.
        Map<String, String> data = Collections.singletonMap("v", "中😀\"\n");
        assertEquals(data, execute(15, true, data).getAppendedWfContext());
        assertEquals(Collections.emptyMap(), execute(14, true, data).getAppendedWfContext());
        assertEquals(1, lengthWarnings());
    }

    @Test
    void ordinaryTaskDoesNotReportWorkflowContext() throws Exception {
        assertNull(execute(1, false, Collections.singletonMap("v", "value")).getAppendedWfContext());
        assertEquals(0, lengthWarnings());
    }

    private static Map<String, String> sizedContext(int jsonLength) {
        // The surrounding {"v":""} contributes exactly eight code units.
        char[] value = new char[jsonLength - 8]; Arrays.fill(value, 'x');
        return Collections.singletonMap("v", new String(value));
    }

    private long lengthWarnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN)
                .filter(e -> e.getFormattedMessage().contains("current length of appended workflow context data"))
                .count();
    }

    private TaskTrackerReportInstanceStatusReq execute(int limit, boolean workflow, Map<String, String> data) throws Exception {
        PowerJobWorkerConfig config = new PowerJobWorkerConfig(); config.setMaxAppendedWfContextLength(limit);
        ExecutorManager executors = new ExecutorManager(config);
        WorkerAppInfo app = new WorkerAppInfo(); app.setAppId(1L);
        WorkerRuntime runtime = new WorkerRuntime(); runtime.setAppInfo(app); runtime.setWorkerConfig(config);
        runtime.setExecutorManager(executors); runtime.setWorkerAddress("127.0.0.1:27777");
        CompletableFuture<TaskTrackerReportInstanceStatusReq> report = new CompletableFuture<>();
        CountDownLatch constructed = new CountDownLatch(1);
        runtime.setProcessorLoader(definition -> new ProcessorBean().setProcessor(context -> {
            if (!constructed.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("tracker construction timed out");
            data.forEach((key, value) -> context.getWorkflowContext().appendData2WfContext(key, value));
            return new ProcessResult(true, "complete");
        }));
        ServerScheduleJobReq request = new ServerScheduleJobReq();
        request.setJobId(1L); request.setInstanceId(2L); request.setWfInstanceId(workflow ? 3L : null);
        request.setExecuteType(ExecuteType.STANDALONE.name()); request.setTaskRetryNum(0);
        request.setInstanceTimeoutMS(0L); request.setMeta("{}");
        LightTaskTracker tracker = null;
        try {
            tracker = new LightTaskTracker(request, runtime) {
                @Override
                protected void reportFinalStatusThenDestroy(WorkerRuntime worker, TaskTrackerReportInstanceStatusReq result) {
                    // Capture the real Processor -> finished-status report boundary without a network server.
                    report.complete(result);
                }
            };
            constructed.countDown();
            TaskTrackerReportInstanceStatusReq result = report.get(5, TimeUnit.SECONDS);
            assertEquals(InstanceStatus.SUCCEED.getV(), result.getInstanceStatus());
            assertEquals("complete", result.getResult());
            assertEquals(1L, result.getSucceedTaskNum());
            return result;
        } finally {
            constructed.countDown();
            if (tracker != null) tracker.destroy();
            executors.getLightweightTaskStatusCheckExecutor().shutdownNow();
            executors.getLightweightTaskExecutorService().shutdownNow();
            executors.shutdown();
            assertTrue(executors.getLightweightTaskExecutorService().awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(executors.getLightweightTaskStatusCheckExecutor().awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
