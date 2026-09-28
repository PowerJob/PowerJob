package tech.powerjob.server.core.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceManager;
import tech.powerjob.server.core.instance.InstanceMetadataService;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.transporter.ProtocolInfo;
import tech.powerjob.server.remote.transporter.TransportService;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InstanceRunningTimeoutRetryTest {
    private final InstanceInfoRepository instances = mock(InstanceInfoRepository.class);
    private final JobInfoRepository jobs = mock(JobInfoRepository.class);
    private final AppInfoRepository apps = mock(AppInfoRepository.class);
    private final TransportService transport = mock(TransportService.class);
    private final InstanceManager completion = mock(InstanceManager.class);
    private InstanceStatusCheckService checker;
    private InstanceInfoDO instance;
    private JobInfoDO job;

    @BeforeEach
    void setUp() {
        ProtocolInfo protocol = new ProtocolInfo();
        protocol.setAddress("fixture-server:10010");
        when(transport.defaultProtocol()).thenReturn(protocol);
        when(apps.listAppIdByCurrentServer(protocol.getAddress())).thenReturn(Collections.singletonList(10L));
        // Real redispatch code exercises the conditional state transition, not just a mocked call.
        DispatchService dispatch = new DispatchService(transport, null, completion, null, instances, null);
        checker = new InstanceStatusCheckService(transport, dispatch, completion,
                mock(WorkflowInstanceManager.class), apps, jobs, instances,
                mock(WorkflowInfoRepository.class), mock(WorkflowInstanceInfoRepository.class));
        instance = new InstanceInfoDO();
        instance.setId(20L); instance.setAppId(10L); instance.setJobId(30L); instance.setInstanceId(40L);
        instance.setStatus(InstanceStatus.RUNNING.getV());
        instance.setRunningTimes(1L); instance.setGmtModified(new Date(1L));
        job = new JobInfoDO();
        job.setId(30L); job.setStatus(SwitchableStatus.ENABLE.getV());
        job.setTimeExpressionType(TimeExpressionType.API.getV()); job.setInstanceRetryNum(1);
        when(jobs.findByIdIn(any())).thenReturn(Collections.singletonList(job));
        when(instances.findById(20L)).thenReturn(Optional.of(instance));
        when(instances.updateStatusAndGmtModifiedByInstanceIdAndOriginStatus(eq(40L),
                eq(InstanceStatus.RUNNING.getV()), eq(InstanceStatus.WAITING_DISPATCH.getV()), any()))
                .thenAnswer(call -> {
                    assertEquals(InstanceStatus.RUNNING.getV(), instance.getStatus());
                    instance.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
                    instance.setGmtModified(call.getArgument(3));
                    return 1;
                });
    }

    private void reportTimedOutInstance() {
        BriefInstanceInfo row = new BriefInstanceInfo(10L, 20L, 30L, 40L, instance.getRunningTimes());
        when(instances.selectBriefInfoByAppIdInAndStatusAndGmtModifiedBefore(anyList(),
                eq(InstanceStatus.RUNNING.getV()), any(Date.class), any()))
                .thenReturn(Collections.singletonList(row), Collections.emptyList());
        checker.checkRunningInstance();
    }

    @ParameterizedTest(name = "{0}: retries={1}, totalAttempts={2}, retry={3}")
    @CsvSource({
            "API,0,1,false", "API,1,1,true", "API,1,2,false",
            "API,2,1,true", "API,2,2,true", "API,2,3,false",
            "CRON,0,1,false", "CRON,1,1,true", "CRON,1,2,false",
            "CRON,2,1,true", "CRON,2,2,true", "CRON,2,3,false"
    })
    void timeoutHonorsConfiguredRetryBudget(TimeExpressionType type, int retryBudget,
                                            long totalAttempts, boolean shouldRetry) {
        job.setTimeExpressionType(type.getV()); job.setInstanceRetryNum(retryBudget);
        instance.setRunningTimes(totalAttempts);
        reportTimedOutInstance();
        assertEquals(totalAttempts, instance.getRunningTimes().longValue(), "Redispatch must not reset attempts");
        if (shouldRetry) {
            assertEquals(InstanceStatus.WAITING_DISPATCH.getV(), instance.getStatus());
            assertNull(instance.getFinishedTime());
            verify(instances, never()).saveAndFlush(any());
            verify(completion, never()).processFinishedInstance(anyLong(), any(), any(), any());
        } else {
            assertTimedOutFailure();
        }
    }

    @ParameterizedTest
    @EnumSource(value = SwitchableStatus.class, names = {"DISABLE", "DELETED"})
    void inactiveJobsStillCannotRetry(SwitchableStatus state) {
        job.setStatus(state.getV()); job.setInstanceRetryNum(2);
        reportTimedOutInstance();
        assertTimedOutFailure();
    }

    @ParameterizedTest
    @EnumSource(value = TimeExpressionType.class, names = {"FIXED_RATE", "FIXED_DELAY"})
    void frequentJobsStillUseTheirOwnRecovery(TimeExpressionType type) {
        job.setTimeExpressionType(type.getV()); job.setInstanceRetryNum(2);
        reportTimedOutInstance();
        assertTimedOutFailure();
    }

    @Test
    void firstAcceptedRunningReportCountsTheInitialAttempt() throws Exception {
        InstanceMetadataService metadata = mock(InstanceMetadataService.class);
        when(metadata.fetchJobInfoByInstanceId(40L)).thenReturn(job);
        when(instances.findByInstanceId(40L)).thenReturn(instance);
        instance.setStatus(InstanceStatus.WAITING_WORKER_RECEIVE.getV());
        instance.setRunningTimes(0L); instance.setLastReportTime(0L);
        instance.setTaskTrackerAddress("fixture-worker:27777");
        TaskTrackerReportInstanceStatusReq request = new TaskTrackerReportInstanceStatusReq();
        request.setInstanceId(40L); request.setInstanceStatus(InstanceStatus.RUNNING.getV());
        request.setSourceAddress("fixture-worker:27777"); request.setReportTime(1L);
        InstanceManager manager = new InstanceManager(null, null, metadata, instances, null, null);
        manager.updateStatus(request);
        assertEquals(1L, instance.getRunningTimes().longValue());
        verify(instances).updateStatusChangeInfoByInstanceIdAndStatus(eq(1L), any(Date.class), eq(1L),
                eq(InstanceStatus.RUNNING.getV()), eq(40L), eq(InstanceStatus.WAITING_WORKER_RECEIVE.getV()));
    }

    private void assertTimedOutFailure() {
        assertEquals(InstanceStatus.FAILED.getV(), instance.getStatus());
        assertEquals(SystemInstanceResult.REPORT_TIMEOUT, instance.getResult());
        assertNotNull(instance.getFinishedTime());
        verify(instances).saveAndFlush(same(instance));
        verify(instances, never()).updateStatusAndGmtModifiedByInstanceIdAndOriginStatus(anyLong(), anyInt(), anyInt(), any());
        verify(completion).processFinishedInstance(40L, null, InstanceStatus.FAILED, SystemInstanceResult.REPORT_TIMEOUT);
    }
}
