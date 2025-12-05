package tech.powerjob.worker.test.function;

import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.worker.background.discovery.PowerJobServerDiscoveryService;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.persistence.DbTaskPersistenceService;
import tech.powerjob.worker.persistence.TaskPersistenceService;
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
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.processor.ProcessorLoader;
import tech.powerjob.worker.background.discovery.ServerDiscoveryService;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

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

        // 初始化 WorkerRuntime 並設置必要的 mock 對象
        WorkerRuntime workerRuntime = new WorkerRuntime();
        workerRuntime.setTransporter(mock(Transporter.class));
        workerRuntime.setProcessorLoader(mock(ProcessorLoader.class));

        ProcessorTracker pt = new ProcessorTracker(req, workerRuntime);
        Thread.sleep(3000);
    }

    @Test
    public void testTaskTrackerProcessorIdle() throws Exception {

        ProcessorTrackerStatusReportReq req = ProcessorTrackerStatusReportReq.buildIdleReport(10086L);
        // 設置 address，避免 ConcurrentHashMap 的 NPE
        req.setAddress("127.0.0.1:27777");

        ServerScheduleJobReq serverScheduleJobReq = TestUtils.genServerScheduleJobReq(ExecuteType.STANDALONE, TimeExpressionType.API);

        // 初始化 WorkerRuntime
        WorkerRuntime workerRuntime = new WorkerRuntime();

        // 設置 AppInfo
        WorkerAppInfo appInfo = new WorkerAppInfo();
        appInfo.setAppId(1L);
        workerRuntime.setAppInfo(appInfo);

        // 設置 Worker 地址
        workerRuntime.setWorkerAddress("127.0.0.1:27777");

        // 設置 transporter (重要 - 避免 NullPointerException)
        Transporter mockTransporter = mock(Transporter.class);
        workerRuntime.setTransporter(mockTransporter);

        // 設置 ServerDiscoveryService (重要 - 避免 NullPointerException)
        ServerDiscoveryService mockServerDiscoveryService = mock(ServerDiscoveryService.class);
        workerRuntime.setServerDiscoveryService(mockServerDiscoveryService);

        // 初始化並設置 TaskPersistenceService
        TaskPersistenceService taskPersistenceService = new DbTaskPersistenceService(StoreStrategy.MEMORY);
        taskPersistenceService.init();
        workerRuntime.setTaskPersistenceService(taskPersistenceService);

        HeavyTaskTracker taskTracker = HeavyTaskTracker.create(serverScheduleJobReq, workerRuntime);
        if (taskTracker != null) {
            taskTracker.receiveProcessorTrackerHeartbeat(req);
        }
    }
}
