package tech.powerjob.server.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.*;
import tech.powerjob.server.core.uid.IdGenerateService;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.transporter.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.controller.InstanceController;
import tech.powerjob.server.web.request.QueryInstanceDetailRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InstanceOwnershipTest {
    @ParameterizedTest
    @CsvSource({"detail,true", "detail,false", "detailPlus,true", "detailPlus,false", "stop,true", "stop,false", "retry,true", "retry,false", "cancel,true", "cancel,false"})
    void eachInstanceEntryPointChecksStoredOwnershipBeforeReadingOrChangingState(String operation, boolean sameApp) {
        InstanceInfoRepository instances = mock(InstanceInfoRepository.class);
        JobInfoRepository jobs = mock(JobInfoRepository.class);
        DispatchService dispatch = mock(DispatchService.class);
        InstanceManager manager = mock(InstanceManager.class);
        InstanceLogService logs = mock(InstanceLogService.class);
        TransportService transport = mock(TransportService.class);
        InstanceService service = new InstanceService(transport, dispatch, mock(IdGenerateService.class), manager, jobs, instances, mock(WorkerClusterQueryService.class), logs);
        InstanceController controller = new InstanceController(); ReflectionTestUtils.setField(controller, "instanceService", service);
        InstanceInfoDO instance = new InstanceInfoDO(); instance.setInstanceId(9007199254740993L); instance.setAppId(20L); instance.setJobId(30L);
        instance.setStatus("retry".equals(operation) ? InstanceStatus.FAILED.getV() : InstanceStatus.WAITING_DISPATCH.getV());
        instance.setExpectedTriggerTime(System.currentTimeMillis() + 3600000L); instance.setJobParams("private-parameters");
        when(instances.findByInstanceId(instance.getInstanceId())).thenReturn(instance);
        when(jobs.findById(30L)).thenReturn(Optional.of(new JobInfoDO()));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("AppId", sameApp ? "20" : "21");
        QueryInstanceDetailRequest body = new QueryInstanceDetailRequest(); body.setInstanceId(instance.getInstanceId()); body.setAppId(20L);
        int previousStatus = instance.getStatus();
        org.junit.jupiter.api.function.Executable action = () -> {
            switch (operation) {
                case "detail": assertEquals("private-parameters", controller.getInstanceDetail(instance.getInstanceId(), request).getData().getJobParams()); break;
                case "detailPlus": assertEquals("private-parameters", controller.getInstanceDetailPlus(body, request).getData().getJobParams()); break;
                case "stop": controller.stopInstance(instance.getInstanceId(), request); break;
                case "retry": controller.retryInstance(instance.getInstanceId(), request); break;
                default: service.cancelInstance(sameApp ? 20L : 21L, instance.getInstanceId());
            }
        };
        if (sameApp) {
            assertDoesNotThrow(action);
            if (operation.equals("stop")) assertEquals(InstanceStatus.STOPPED.getV(), instance.getStatus());
            if (operation.equals("retry")) assertEquals(InstanceStatus.WAITING_DISPATCH.getV(), instance.getStatus());
            if (operation.equals("cancel")) assertEquals(InstanceStatus.CANCELED.getV(), instance.getStatus());
        } else {
            assertThrows(PowerJobException.class, action);
            assertEquals(previousStatus, instance.getStatus()); assertEquals("private-parameters", instance.getJobParams());
            verify(instances, never()).saveAndFlush(any()); verifyNoInteractions(dispatch, manager, logs, transport);
        }
    }
}
