package tech.powerjob.server.core.workflow.hanlder.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.workflow.hanlder.TaskNodeHandler;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;

import javax.annotation.Resource;

/**
 * @author Echo009
 * @since 2021/12/9
 */
@Slf4j
@Component
public class JobNodeHandler implements TaskNodeHandler {

    @Resource
    private InstanceService instanceService;

    @Resource
    private JobInfoRepository jobInfoRepository;

    @Resource
    private DispatchService dispatchService;

    @Override
    public void createTaskInstance(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {
        // instanceParam 传递的是工作流实例的 wfContext
        Long instanceId = instanceService.create(node.getJobId(), wfInstanceInfo.getAppId(), node.getNodeParams(), wfInstanceInfo.getWfContext(), wfInstanceInfo.getWfInstanceId(), System.currentTimeMillis());
        node.setInstanceId(instanceId);
        node.setStatus(InstanceStatus.RUNNING.getV());
        log.info("[Workflow-{}|{}] create readyNode(JOB) instance(nodeId={},jobId={},instanceId={}) successfully~", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), node.getJobId(), instanceId);
    }

    @Override
    public void startTaskInstance(PEWorkflowDAG.Node node) {
        JobInfoDO jobInfo = jobInfoRepository.findById(node.getJobId()).orElseGet(JobInfoDO::new);
        // 洗去时间表达式类型
        jobInfo.setTimeExpressionType(TimeExpressionType.WORKFLOW.getV());
        dispatchService.dispatch(jobInfo, node.getInstanceId());
    }

    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.JOB;
    }
}
