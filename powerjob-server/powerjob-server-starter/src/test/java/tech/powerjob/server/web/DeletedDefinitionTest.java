package tech.powerjob.server.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.http.*;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.scheduler.TimingStrategyService;
import tech.powerjob.server.core.service.impl.job.JobServiceImpl;
import tech.powerjob.server.core.workflow.WorkflowService;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeletedDefinitionTest {
    @ParameterizedTest
    @ValueSource(strings = {"save", "enable", "disable", "run", "copy"})
    void deletedJobCannotBeModifiedOrExecuted(String operation) {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        InstanceService instances = mock(InstanceService.class);
        DispatchService dispatch = mock(DispatchService.class);
        JobServiceImpl service = new JobServiceImpl(instances, dispatch, repository, mock(InstanceInfoRepository.class), mock(TimingStrategyService.class));
        JobInfoDO job = new JobInfoDO(); job.setId(10L); job.setAppId(20L); job.setStatus(SwitchableStatus.DELETED.getV());
        when(repository.findById(10L)).thenReturn(Optional.of(job));
        SaveJobInfoRequest save = new SaveJobInfoRequest(); save.setId(10L); save.setAppId(20L); save.setJobName("fixture");
        save.setProcessorInfo("fixture.Processor"); save.setProcessorType(ProcessorType.BUILT_IN);
        save.setExecuteType(ExecuteType.STANDALONE); save.setTimeExpressionType(TimeExpressionType.API);
        assertThrows(RuntimeException.class, () -> {
            switch (operation) {
                case "save": service.saveJob(save); break;
                case "enable": service.enableJob(10L); break;
                case "disable": service.disableJob(10L); break;
                case "run": service.runJob(20L, new RunJobRequest().setAppId(20L).setJobId(10L).setDelay(3600000L)); break;
                default: service.copyJob(10L);
            }
        });
        assertEquals(SwitchableStatus.DELETED.getV(), job.getStatus());
        verify(repository, never()).saveAndFlush(any()); verifyNoInteractions(instances, dispatch);
        // Repeated deletion remains idempotent, including cleanup callers.
        job.setTimeExpressionType(TimeExpressionType.API.getV());
        assertDoesNotThrow(() -> service.deleteJob(10L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"save", "enable", "disable", "run", "copy"})
    void deletedWorkflowCannotBeModifiedOrExecuted(String operation) {
        WorkflowInfoRepository repository = mock(WorkflowInfoRepository.class);
        WorkflowService service = new WorkflowService(); ReflectionTestUtils.setField(service, "workflowInfoRepository", repository);
        WorkflowInfoDO workflow = new WorkflowInfoDO(); workflow.setId(10L); workflow.setAppId(20L); workflow.setStatus(SwitchableStatus.DELETED.getV());
        when(repository.findById(10L)).thenReturn(Optional.of(workflow));
        SaveWorkflowRequest save = new SaveWorkflowRequest(); save.setId(10L); save.setAppId(20L); save.setWfName("fixture"); save.setTimeExpressionType(TimeExpressionType.API);
        RuntimeException rejection = assertThrows(RuntimeException.class, () -> {
            switch (operation) {
                case "save": service.saveWorkflow(save); break;
                case "enable": service.enableWorkflow(10L, 20L); break;
                case "disable": service.disableWorkflow(10L, 20L); break;
                case "run": service.runWorkflow(10L, 20L, null, 3600000L); break;
                default: service.copyWorkflow(10L, 20L);
            }
        });
        assertTrue(rejection instanceof PowerJobException || rejection instanceof IllegalStateException);
        assertEquals(SwitchableStatus.DELETED.getV(), workflow.getStatus()); verify(repository, never()).saveAndFlush(any());
        assertDoesNotThrow(() -> service.deleteWorkflow(10L, 20L));
    }
}
