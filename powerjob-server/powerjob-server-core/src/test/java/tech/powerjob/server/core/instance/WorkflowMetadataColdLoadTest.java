package tech.powerjob.server.core.instance;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.server.common.constants.InstanceType;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowMetadataColdLoadTest {

    @Test
    void coldCacheRestoresWorkflowTypeFromWorkflowInstanceId() throws Exception {
        InstanceInfoDO child = new InstanceInfoDO();
        child.setWfInstanceId(999L);
        assertWorkflowCopy(child);
    }

    @Test
    void coldCacheRestoresWorkflowTypeFromPersistedInstanceType() throws Exception {
        InstanceInfoDO child = new InstanceInfoDO();
        child.setType(InstanceType.WORKFLOW.getV());
        assertWorkflowCopy(child);
    }

    @Test
    void ordinaryInstanceKeepsDefinitionAndExistingCacheBehavior() throws Exception {
        JobInfoDO definition = definition();
        InstanceMetadataService service = service(new InstanceInfoDO(), definition);
        assertSame(definition, service.fetchJobInfoByInstanceId(100L));
        assertSame(definition, service.fetchJobInfoByInstanceId(100L));
        assertEquals(TimeExpressionType.FIXED_RATE.getV(), definition.getTimeExpressionType());
    }

    private void assertWorkflowCopy(InstanceInfoDO child) throws Exception {
        JobInfoDO definition = definition();
        InstanceMetadataService service = service(child, definition);
        JobInfoDO loaded = service.fetchJobInfoByInstanceId(100L);
        assertNotSame(definition, loaded);
        assertEquals(TimeExpressionType.WORKFLOW.getV(), loaded.getTimeExpressionType());
        assertEquals(TimeExpressionType.FIXED_RATE.getV(), definition.getTimeExpressionType());
        assertSame(loaded, service.fetchJobInfoByInstanceId(100L));
        service.invalidateJobInfo(100L);
        assertEquals(TimeExpressionType.WORKFLOW.getV(), service.fetchJobInfoByInstanceId(100L).getTimeExpressionType());
    }

    private JobInfoDO definition() {
        JobInfoDO definition = new JobInfoDO();
        definition.setTimeExpressionType(TimeExpressionType.FIXED_RATE.getV());
        return definition;
    }

    private InstanceMetadataService service(InstanceInfoDO instance, JobInfoDO definition) throws Exception {
        JobInfoRepository jobs = mock(JobInfoRepository.class);
        InstanceInfoRepository instances = mock(InstanceInfoRepository.class);
        instance.setJobId(7L);
        when(jobs.findById(7L)).thenReturn(Optional.of(definition));
        when(instances.findByInstanceId(100L)).thenReturn(instance);
        InstanceMetadataService service = new InstanceMetadataService(jobs, instances);
        ReflectionTestUtils.setField(service, "instanceMetadataCacheSize", 10);
        service.afterPropertiesSet();
        return service;
    }
}
