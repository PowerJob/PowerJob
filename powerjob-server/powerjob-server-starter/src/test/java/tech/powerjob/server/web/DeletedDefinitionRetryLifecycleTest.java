package tech.powerjob.server.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.alarm.AlarmCenter;
import tech.powerjob.server.core.instance.*;
import tech.powerjob.server.core.scheduler.InstanceStatusCheckService;
import tech.powerjob.server.core.scheduler.TimingStrategyService;
import tech.powerjob.server.core.service.impl.job.JobServiceImpl;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.transporter.ProtocolInfo;
import tech.powerjob.server.remote.transporter.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.remote.worker.selector.TaskTrackerSelectorService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeletedDefinitionRetryLifecycleTest {
    @ParameterizedTest(name = "{0}: budget={1}, lastReport={2}")
    @CsvSource({"API,0,FAILED", "API,1,FAILED", "API,1,SUCCEED",
            "CRON,0,FAILED", "CRON,1,FAILED", "CRON,1,SUCCEED",
            "WORKFLOW,0,FAILED", "WORKFLOW,1,FAILED", "WORKFLOW,1,SUCCEED"})
    void deletingDefinitionDoesNotInterceptExistingFailureRetry(TimeExpressionType type, int budget,
                                                               InstanceStatus finalStatus) throws Exception {
        InstanceInfoRepository instances = mock(InstanceInfoRepository.class);
        JobInfoRepository jobs = mock(JobInfoRepository.class);
        AppInfoRepository apps = mock(AppInfoRepository.class);
        TransportService transport = mock(TransportService.class);
        InstanceMetadataService metadata = mock(InstanceMetadataService.class);
        WorkerClusterQueryService workers = mock(WorkerClusterQueryService.class);
        TaskTrackerSelectorService selector = mock(TaskTrackerSelectorService.class);
        InstanceManager manager = spy(new InstanceManager(mock(AlarmCenter.class), mock(InstanceLogService.class),
                metadata, instances, mock(WorkflowInstanceManager.class), workers));
        // Leave asynchronous log/alarm delivery outside this state-transition test.
        doNothing().when(manager).processFinishedInstance(anyLong(), any(), any(), any());
        DispatchService dispatch = new DispatchService(transport, workers, manager, metadata, instances, selector);
        InstanceInfoDO instance = new InstanceInfoDO(); instance.setId(20L); instance.setInstanceId(40L);
        instance.setAppId(10L); instance.setJobId(30L); instance.setStatus(InstanceStatus.RUNNING.getV());
        instance.setRunningTimes(1L); instance.setLastReportTime(1L); instance.setTaskTrackerAddress("worker-one:27777");
        JobInfoDO job = new JobInfoDO(); job.setId(30L); job.setAppId(10L); job.setStatus(SwitchableStatus.ENABLE.getV());
        job.setTimeExpressionType(type.getV()); job.setInstanceRetryNum(budget); job.setMaxInstanceNum(1);
        job.setConcurrency(1); job.setTaskRetryNum(0);
        job.setExecuteType(ExecuteType.STANDALONE.getV()); job.setProcessorType(ProcessorType.BUILT_IN.getV());
        when(instances.findByInstanceId(40L)).thenReturn(instance);
        when(jobs.findById(30L)).thenReturn(Optional.of(job));
        when(jobs.findByIdIn(any())).thenReturn(Collections.singletonList(job));
        when(metadata.fetchJobInfoByInstanceId(40L)).thenReturn(job);
        when(instances.updateStatusChangeInfoByInstanceIdAndStatus(anyLong(), any(), anyLong(), anyInt(), eq(40L), anyInt())).thenReturn(1);

        JobServiceImpl definitions = new JobServiceImpl(mock(InstanceService.class), dispatch, jobs, instances, mock(TimingStrategyService.class));
        definitions.deleteJob(30L);
        assertEquals(SwitchableStatus.DELETED.getV(), job.getStatus());
        assertEquals(InstanceStatus.RUNNING.getV(), instance.getStatus());
        manager.updateStatus(report(2L, InstanceStatus.FAILED, instance.getTaskTrackerAddress()));

        if (budget > 0) {
            assertEquals(InstanceStatus.WAITING_DISPATCH.getV(), instance.getStatus());
            assertEquals(1L, instance.getRunningTimes());
            assertTrue(instance.getExpectedTriggerTime() > System.currentTimeMillis());
            verify(manager, never()).processFinishedInstance(anyLong(), any(), any(), any());
            // Model the next overdue DB scan without sleeping for the scheduler's polling interval.
            when(instances.findAllByAppIdInAndStatusAndExpectedTriggerTimeLessThan(anyList(), eq(InstanceStatus.WAITING_DISPATCH.getV()), anyLong(), any()))
                    .thenReturn(Collections.singletonList(instance), Collections.emptyList());
            ProtocolInfo protocol = new ProtocolInfo(); protocol.setAddress("server-two:10010");
            when(transport.defaultProtocol()).thenReturn(protocol);
            when(apps.listAppIdByCurrentServer(protocol.getAddress())).thenReturn(Collections.singletonList(10L));
            WorkerInfo worker = new WorkerInfo(); worker.setAddress("worker-two:27777"); worker.setProtocol("HTTP");
            when(workers.geAvailableWorkers(job)).thenReturn(Collections.singletonList(worker));
            when(selector.select(eq(job), eq(instance), anyList())).thenReturn(worker);
            when(instances.update4TriggerSucceed(eq(40L), eq(InstanceStatus.WAITING_WORKER_RECEIVE.getV()), anyLong(), eq(worker.getAddress()), any(), eq(InstanceStatus.WAITING_DISPATCH.getV())))
                    .thenAnswer(call -> { instance.setStatus(call.getArgument(1)); instance.setTaskTrackerAddress(call.getArgument(3)); return 1; });
            InstanceStatusCheckService checker = new InstanceStatusCheckService(transport, dispatch, manager,
                    mock(WorkflowInstanceManager.class), apps, jobs, instances,
                    mock(WorkflowInfoRepository.class), mock(WorkflowInstanceInfoRepository.class));
            checker.checkWaitingDispatchInstance();
            assertEquals(InstanceStatus.WAITING_WORKER_RECEIVE.getV(), instance.getStatus());
            ArgumentCaptor<ServerScheduleJobReq> request = ArgumentCaptor.forClass(ServerScheduleJobReq.class);
            verify(transport).tell(eq("HTTP"), any(), request.capture());
            assertEquals(40L, request.getValue().getInstanceId());
            assertEquals(30L, request.getValue().getJobId());
            assertEquals(Collections.singletonList(worker.getAddress()), request.getValue().getAllWorkerAddress());
            manager.updateStatus(report(3L, finalStatus, worker.getAddress()));
            assertEquals(2L, instance.getRunningTimes());
        } else {
            verifyNoInteractions(transport);
        }
        assertEquals(finalStatus.getV(), instance.getStatus());
        assertNotNull(instance.getFinishedTime());
        assertEquals("fixture-result", instance.getResult());
        verify(instances).saveAndFlush(instance);
        verify(manager).processFinishedInstance(40L, null, finalStatus, "fixture-result");
        assertEquals(SwitchableStatus.DELETED.getV(), job.getStatus(), "Retry must not revive the definition");
    }

    private TaskTrackerReportInstanceStatusReq report(long timestamp, InstanceStatus status, String worker) {
        TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
        req.setInstanceId(40L); req.setJobId(30L); req.setAppId(10L); req.setReportTime(timestamp);
        req.setSourceAddress(worker); req.setInstanceStatus(status.getV()); req.setResult("fixture-result");
        return req;
    }
}
