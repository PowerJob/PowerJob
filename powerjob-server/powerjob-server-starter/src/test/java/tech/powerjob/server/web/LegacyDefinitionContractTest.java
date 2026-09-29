package tech.powerjob.server.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.scheduler.TimingStrategyService;
import tech.powerjob.server.core.service.impl.job.JobServiceImpl;
import tech.powerjob.server.core.validator.JobNodeValidator;
import tech.powerjob.server.core.validator.NestedWorkflowNodeValidator;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LegacyDefinitionContractTest {
    @ParameterizedTest
    @ValueSource(longs = {101L, 202L})
    void jobSaveRetainsCallerProvidedAppId(long previousApp) {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        JobServiceImpl service = new JobServiceImpl(mock(InstanceService.class), mock(DispatchService.class), repository,
                mock(InstanceInfoRepository.class), mock(TimingStrategyService.class));
        JobInfoDO existing = new JobInfoDO(); existing.setId(303L); existing.setAppId(previousApp); existing.setStatus(1);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        SaveJobInfoRequest req = new SaveJobInfoRequest(); req.setId(303L); req.setAppId(101L); req.setJobName("fixture");
        req.setProcessorInfo("fixture.Processor"); req.setProcessorType(ProcessorType.BUILT_IN);
        req.setExecuteType(ExecuteType.STANDALONE); req.setTimeExpressionType(TimeExpressionType.API);
        assertEquals(303L, service.saveJob(req));
        assertEquals(101L, existing.getAppId());
        verify(repository).saveAndFlush(existing);
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 202L})
    void jobNodeKeepsLegacyCrossApplicationReference(long owner) {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        JobInfoDO job = new JobInfoDO(); job.setAppId(owner); job.setStatus(1);
        when(repository.findById(303L)).thenReturn(Optional.of(job));
        WorkflowNodeInfoDO node = new WorkflowNodeInfoDO(); node.setAppId(101L); node.setJobId(303L);
        JobNodeValidator validator = new JobNodeValidator(repository);
        assertDoesNotThrow(() -> validator.simpleValidate(node));
        job.setStatus(SwitchableStatus.DELETED.getV());
        assertThrows(RuntimeException.class, () -> validator.simpleValidate(node));
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 202L})
    void nestedWorkflowKeepsLegacyCrossApplicationReference(long owner) {
        WorkflowInfoRepository repository = mock(WorkflowInfoRepository.class);
        WorkflowInfoDO workflow = new WorkflowInfoDO(); workflow.setAppId(owner); workflow.setStatus(1);
        workflow.setPeDAG("{\"nodes\":[],\"edges\":[]}");
        when(repository.findById(303L)).thenReturn(Optional.of(workflow));
        WorkflowNodeInfoDO node = new WorkflowNodeInfoDO(); node.setAppId(101L); node.setJobId(303L);
        NestedWorkflowNodeValidator validator = new NestedWorkflowNodeValidator(repository, mock(WorkflowNodeInfoRepository.class));
        assertDoesNotThrow(() -> validator.simpleValidate(node));
        workflow.setStatus(SwitchableStatus.DELETED.getV());
        assertThrows(RuntimeException.class, () -> validator.simpleValidate(node));
    }
}
