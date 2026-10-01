package tech.powerjob.server.core.workflow;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.GenericApplicationContext;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.common.utils.SpringUtils;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.workflow.hanlder.impl.JobNodeHandler;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobNodeDefinitionIsolationTest {
    @Test
    void workflowDispatchOverrideDoesNotModifyItsDefinition() throws Exception {
        JobInfoRepository repository = mock(JobInfoRepository.class);
        DispatchService dispatcher = mock(DispatchService.class);
        JobInfoDO definition = new JobInfoDO();
        definition.setId(7L);
        definition.setTimeExpressionType(TimeExpressionType.CRON.getV());
        when(repository.findById(7L)).thenReturn(Optional.of(definition));
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("fixtureDispatch", dispatcher);
        context.refresh();
        Field field = SpringUtils.class.getDeclaredField("context");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            new SpringUtils().setApplicationContext(context);
            new JobNodeHandler(repository).startTaskInstance(new PEWorkflowDAG.Node().setJobId(7L).setInstanceId(100L));
            ArgumentCaptor<JobInfoDO> sent = ArgumentCaptor.forClass(JobInfoDO.class);
            verify(dispatcher).dispatch(sent.capture(), eq(100L), any(), any());
            assertEquals(TimeExpressionType.WORKFLOW.getV(), sent.getValue().getTimeExpressionType().intValue());
            assertEquals(TimeExpressionType.CRON.getV(), definition.getTimeExpressionType().intValue());
            assertEquals(definition.getId(), sent.getValue().getId());
            assertNotSame(definition, sent.getValue());
        } finally {
            field.set(null, previous);
            context.close();
        }
    }
}
