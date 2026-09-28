package tech.powerjob.server.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.http.*;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.container.ContainerService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.scheduler.TimingStrategyService;
import tech.powerjob.server.core.service.NodeValidateService;
import tech.powerjob.server.core.service.impl.job.JobServiceImpl;
import tech.powerjob.server.core.validator.JobNodeValidator;
import tech.powerjob.server.core.validator.NestedWorkflowNodeValidator;
import tech.powerjob.server.core.workflow.WorkflowService;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantWriteOwnershipTest {
    private SaveJobInfoRequest jobRequest(Long id) {
        SaveJobInfoRequest request = new SaveJobInfoRequest();
        request.setId(id); request.setAppId(101L); request.setJobName("fixture");
        request.setProcessorInfo("fixture.Processor"); request.setProcessorType(ProcessorType.BUILT_IN);
        request.setExecuteType(ExecuteType.STANDALONE); request.setTimeExpressionType(TimeExpressionType.API);
        return request;
    }

    @Test
    void updatingJobCannotMoveAnotherAppsRecord() {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        JobServiceImpl service = new JobServiceImpl(mock(InstanceService.class), mock(DispatchService.class), repository,
                mock(InstanceInfoRepository.class), mock(TimingStrategyService.class));
        JobInfoDO existing = new JobInfoDO(); existing.setId(303L); existing.setAppId(202L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        assertThrows(PowerJobException.class, () -> service.saveJob(jobRequest(303L)));
        assertEquals(202L, existing.getAppId()); verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sameAppJobCreationAndUpdateRemainSupported(boolean update) {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        JobServiceImpl service = new JobServiceImpl(mock(InstanceService.class), mock(DispatchService.class), repository,
                mock(InstanceInfoRepository.class), mock(TimingStrategyService.class));
        JobInfoDO existing = new JobInfoDO(); existing.setId(303L); existing.setAppId(101L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any())).thenAnswer(i -> { JobInfoDO saved = i.getArgument(0); saved.setId(303L); return saved; });
        assertEquals(303L, service.saveJob(jobRequest(update ? 303L : null)));
        verify(repository).saveAndFlush(any());
    }

    @Test
    void workflowUpdateRejectsDifferentOwnerBeforeCopyingProperties() {
        WorkflowService service = new WorkflowService();
        WorkflowInfoRepository repository = mock(WorkflowInfoRepository.class);
        ReflectionTestUtils.setField(service, "workflowInfoRepository", repository);
        WorkflowInfoDO existing = new WorkflowInfoDO(); existing.setId(303L); existing.setAppId(202L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        SaveWorkflowRequest request = new SaveWorkflowRequest(); request.setId(303L); request.setAppId(101L);
        request.setWfName("fixture"); request.setTimeExpressionType(TimeExpressionType.API);
        assertThrows(PowerJobException.class, () -> service.saveWorkflow(request));
        assertEquals(202L, existing.getAppId()); verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void workflowNodeUpdateCannotMoveAnotherAppsRecord() {
        WorkflowService service = new WorkflowService();
        WorkflowNodeInfoRepository repository = mock(WorkflowNodeInfoRepository.class);
        ReflectionTestUtils.setField(service, "workflowNodeInfoRepository", repository);
        WorkflowNodeInfoDO existing = new WorkflowNodeInfoDO(); existing.setId(303L); existing.setAppId(202L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        SaveWorkflowNodeRequest request = new SaveWorkflowNodeRequest(); request.setId(303L); request.setAppId(101L); request.setType(2);
        assertThrows(PowerJobException.class, () -> service.saveWorkflowNode(Collections.singletonList(request)));
        assertEquals(202L, existing.getAppId()); verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sameAppNodeCreationAndUpdateRemainSupported(boolean update) {
        WorkflowService service = new WorkflowService();
        WorkflowNodeInfoRepository repository = mock(WorkflowNodeInfoRepository.class);
        ReflectionTestUtils.setField(service, "workflowNodeInfoRepository", repository);
        ReflectionTestUtils.setField(service, "nodeValidateService", mock(NodeValidateService.class));
        WorkflowNodeInfoDO existing = new WorkflowNodeInfoDO(); existing.setId(303L); existing.setAppId(101L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        SaveWorkflowNodeRequest request = new SaveWorkflowNodeRequest(); request.setId(update ? 303L : null); request.setAppId(101L); request.setType(2);
        assertEquals(1, service.saveWorkflowNode(Collections.singletonList(request)).size());
        verify(repository).saveAndFlush(any());
    }

    @Test
    void containerUpdateRejectsDifferentOwner() {
        ContainerService service = new ContainerService();
        ContainerInfoRepository repository = mock(ContainerInfoRepository.class);
        ReflectionTestUtils.setField(service, "containerInfoRepository", repository);
        ContainerInfoDO existing = new ContainerInfoDO(); existing.setId(303L); existing.setAppId(202L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        ContainerInfoDO request = new ContainerInfoDO(); request.setId(303L); request.setAppId(101L);
        assertThrows(PowerJobException.class, () -> service.save(request));
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sameAppContainerCreationAndUpdateRemainSupported(boolean update) {
        ContainerService service = new ContainerService();
        ContainerInfoRepository repository = mock(ContainerInfoRepository.class);
        ReflectionTestUtils.setField(service, "containerInfoRepository", repository);
        ContainerInfoDO existing = new ContainerInfoDO(); existing.setId(303L); existing.setAppId(101L);
        when(repository.findById(303L)).thenReturn(Optional.of(existing));
        ContainerInfoDO request = new ContainerInfoDO(); request.setId(update ? 303L : null); request.setAppId(101L); request.setSourceType(1);
        assertDoesNotThrow(() -> service.save(request)); verify(repository).saveAndFlush(request);
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 202L})
    void jobNodeReferencesStayWithinTheirApp(long owner) {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        JobInfoDO job = new JobInfoDO(); job.setAppId(owner); job.setStatus(1);
        when(repository.findById(303L)).thenReturn(Optional.of(job));
        WorkflowNodeInfoDO node = new WorkflowNodeInfoDO(); node.setAppId(101L); node.setJobId(303L);
        JobNodeValidator validator = new JobNodeValidator(repository);
        if (owner == 101L) assertDoesNotThrow(() -> validator.simpleValidate(node));
        else assertThrows(PowerJobException.class, () -> validator.simpleValidate(node));
    }

    @ParameterizedTest
    @ValueSource(longs = {101L, 202L})
    void nestedWorkflowReferencesStayWithinTheirApp(long owner) {
        WorkflowInfoRepository repository = mock(WorkflowInfoRepository.class);
        WorkflowInfoDO workflow = new WorkflowInfoDO(); workflow.setAppId(owner); workflow.setStatus(1); workflow.setPeDAG("{\"nodes\":[],\"edges\":[]}");
        when(repository.findById(303L)).thenReturn(Optional.of(workflow));
        WorkflowNodeInfoDO node = new WorkflowNodeInfoDO(); node.setAppId(101L); node.setJobId(303L);
        NestedWorkflowNodeValidator validator = new NestedWorkflowNodeValidator(repository, mock(WorkflowNodeInfoRepository.class));
        if (owner == 101L) assertDoesNotThrow(() -> validator.simpleValidate(node));
        else assertThrows(PowerJobException.class, () -> validator.simpleValidate(node));
    }
}
