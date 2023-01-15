package tech.powerjob.server.core.validator;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowNodeInfoRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Echo009
 * @since 2021/12/14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NestedWorkflowNodeValidator implements NodeValidator {

    private final WorkflowInfoRepository workflowInfoRepository;

    private final WorkflowNodeInfoRepository workflowNodeInfoRepository;

    @Override
    public void complexValidate(WorkflowNodeInfoDO node, WorkflowDAG dag) {
        // 这里检查是否循环嵌套(自身引用自身)
        if (Objects.equals(node.getJobId(), node.getWorkflowId())) {
            throw new PowerJobException("Illegal nested workflow node,Prohibit circular references!" + node.getNodeName());
        }
    }


    @Override
    public void simpleValidate(WorkflowNodeInfoDO node) {
        // 判断对应工作流是否存在
        WorkflowInfoDO workflowInfo = workflowInfoRepository.findById(node.getJobId())
                .orElseThrow(() -> new PowerJobException("Illegal nested workflow node,specified workflow is not exist,node name : " + node.getNodeName()));
        if (workflowInfo.getStatus() == SwitchableStatus.DELETED.getV()) {
            throw new PowerJobException("Illegal nested workflow node,specified workflow has been deleted,node name : " + node.getNodeName());
        }
        // 不允许多层嵌套，即 嵌套工作流节点引用的工作流中不能包含嵌套节点
        PEWorkflowDAG peDag = JSON.parseObject(workflowInfo.getPeDAG(), PEWorkflowDAG.class);
        for (PEWorkflowDAG.Node peDagNode : peDag.getNodes()) {
            //
            final Optional<WorkflowNodeInfoDO> nestWfNodeOp = workflowNodeInfoRepository.findById(peDagNode.getNodeId());
            if (!nestWfNodeOp.isPresent()) {
                // 嵌套的工作流无效，缺失节点元数据
                throw new PowerJobException("Illegal nested workflow node,specified workflow is invalidate,node name : " + node.getNodeName());
            }
            if (Objects.equals(nestWfNodeOp.get().getType(), WorkflowNodeType.NESTED_WORKFLOW.getCode())) {
                throw new PowerJobException("Illegal nested workflow node,specified workflow must be a simple workflow,node name : " + node.getNodeName());
            }
        }
    }

    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.NESTED_WORKFLOW;
    }
}
