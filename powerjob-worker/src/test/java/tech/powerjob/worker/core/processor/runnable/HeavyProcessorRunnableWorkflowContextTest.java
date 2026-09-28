package tech.powerjob.worker.core.processor.runnable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.ThreadLocalStore;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.ProcessorReportTaskStatusReq;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeavyProcessorRunnableWorkflowContextTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(HeavyProcessorRunnable.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(logs);
        logs.stop();
        ThreadLocalStore.clear();
    }

    @Test
    void workflowWithoutAppendedContextDoesNotWarnOnStatusReports() throws InterruptedException {
        List<ProcessorReportTaskStatusReq> reports = executeWorkflow(8192, context -> new ProcessResult(true));

        assertEquals(0, contextLengthWarnings());
        assertNull(reports.get(0).getAppendedWfContext());
        assertEquals(Collections.emptyMap(), reports.get(1).getAppendedWfContext());
    }

    @Test
    void workflowReportsContextAtTheLengthLimit() throws InterruptedException {
        List<ProcessorReportTaskStatusReq> reports = executeWorkflow(15, context -> {
            context.getWorkflowContext().appendData2WfContext("key", "value");
            return new ProcessResult(true);
        });

        assertEquals(0, contextLengthWarnings());
        assertNull(reports.get(0).getAppendedWfContext());
        assertEquals(Collections.singletonMap("key", "value"), reports.get(1).getAppendedWfContext());
    }

    @Test
    void workflowWarnsAndDiscardsOnlyOversizedContext() throws InterruptedException {
        List<ProcessorReportTaskStatusReq> reports = executeWorkflow(14, context -> {
            context.getWorkflowContext().appendData2WfContext("key", "value");
            return new ProcessResult(true);
        });

        assertEquals(1, contextLengthWarnings());
        assertNull(reports.get(0).getAppendedWfContext());
        assertEquals(Collections.emptyMap(), reports.get(1).getAppendedWfContext());
    }

    private long contextLengthWarnings() {
        return logs.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .filter(event -> event.getFormattedMessage().contains("current length of appended workflow context data"))
                .count();
    }

    private List<ProcessorReportTaskStatusReq> executeWorkflow(int maxLength, BasicProcessor processor) throws InterruptedException {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setInstanceId(1L);
        instanceInfo.setWfInstanceId(2L);
        instanceInfo.setExecuteType(ExecuteType.STANDALONE.name());

        TaskDO task = new TaskDO();
        task.setInstanceId(1L);
        task.setSubInstanceId(1L);
        task.setTaskId("0");
        task.setTaskName("workflow-task");
        task.setFailedCnt(0);

        PowerJobWorkerConfig config = new PowerJobWorkerConfig();
        config.setMaxAppendedWfContextLength(maxLength);
        CapturingTransporter transporter = new CapturingTransporter();
        WorkerRuntime runtime = new WorkerRuntime();
        runtime.setWorkerConfig(config);
        runtime.setTransporter(transporter);
        Queue<ProcessorReportTaskStatusReq> retries = new ArrayDeque<>();

        HeavyProcessorRunnable runnable = new HeavyProcessorRunnable(instanceInfo, "127.0.0.1:27777", task,
                new ProcessorBean().setProcessor(processor), null, retries, runtime);
        runnable.innerRun();

        assertTrue(retries.isEmpty());
        assertEquals(2, transporter.reports.size());
        assertEquals(TaskStatus.WORKER_PROCESSING.getValue(), transporter.reports.get(0).getStatus());
        assertEquals(TaskStatus.WORKER_PROCESS_SUCCESS.getValue(), transporter.reports.get(1).getStatus());
        return transporter.reports;
    }

    private static class CapturingTransporter implements Transporter {

        private final List<ProcessorReportTaskStatusReq> reports = new ArrayList<>();

        @Override
        public Protocol getProtocol() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void tell(URL url, PowerSerializable request) {
            reports.add((ProcessorReportTaskStatusReq) request);
        }

        @Override
        public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) {
            reports.add((ProcessorReportTaskStatusReq) request);
            return CompletableFuture.completedFuture(clz.cast(AskResponse.succeed(null)));
        }
    }
}
