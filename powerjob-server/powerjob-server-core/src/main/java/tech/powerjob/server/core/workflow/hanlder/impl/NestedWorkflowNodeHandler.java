package tech.powerjob.server.core.workflow.hanlder.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.WorkflowInstanceStatus;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.core.workflow.hanlder.TaskNodeHandler;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInstanceInfoRepository;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Echo009
 * @since 2021/12/13
 */
@Component
@Slf4j
public class NestedWorkflowNodeHandler implements TaskNodeHandler {

    @Resource
    private WorkflowInfoRepository workflowInfoRepository;

    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    @Resource
    private WorkflowInstanceManager workflowInstanceManager;

    @Override
    public void createTaskInstance(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {
        // check
        Long wfId = node.getJobId();
        WorkflowInfoDO targetWf = workflowInfoRepository.findById(wfId).orElse(null);
        if (targetWf == null || targetWf.getStatus() == SwitchableStatus.DELETED.getV()) {
            if (targetWf == null) {
                log.error("[Workflow-{}|{}] invalid nested workflow node({}),target workflow({}) is not exist!", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), node.getJobId());
            } else {
                log.error("[Workflow-{}|{}] invalid nested workflow node({}),target workflow({}) has been deleted!", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), node.getJobId());
            }
            throw new PowerJobException("invalid nested workflow node," + node.getNodeId());
        }
        if (node.getInstanceId() != null) {
            // 处理重试的情形，不需要创建实例，仅需要更改对应实例的状态
            WorkflowInstanceInfoDO wfInstance = workflowInstanceInfoRepository.findByWfInstanceId(node.getInstanceId()).orElse(null);
            if (wfInstance == null) {
                log.error("[Workflow-{}|{}] invalid nested workflow node({}),target workflow instance({}) is not exist!", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), node.getInstanceId());
                throw new PowerJobException("invalid workflow instance id" + node.getNodeId());
            }
            wfInstance.setStatus(WorkflowInstanceStatus.WAITING.getV());
            wfInstance.setGmtModified(new Date());
            workflowInstanceInfoRepository.saveAndFlush(wfInstance);
        } else {
            // 透传当前的上下文创建新的工作流实例
            String wfContext = wfInstanceInfo.getWfContext();
            Long instanceId = workflowInstanceManager.create(targetWf, wfContext, System.currentTimeMillis(), wfInstanceInfo.getWfInstanceId());
            node.setInstanceId(instanceId);
        }
        node.setStartTime(CommonUtils.formatTime(System.currentTimeMillis()));
        node.setStatus(InstanceStatus.RUNNING.getV());
    }

    @Override
    public void startTaskInstance(PEWorkflowDAG.Node node) {
        Long wfId = node.getJobId();
        WorkflowInfoDO targetWf = workflowInfoRepository.findById(wfId).orElse(null);
        workflowInstanceManager.start(targetWf, node.getInstanceId());
    }

    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.NESTED_WORKFLOW;
    }
}
