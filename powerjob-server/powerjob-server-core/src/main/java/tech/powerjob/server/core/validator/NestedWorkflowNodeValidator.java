package tech.powerjob.server.core.validator;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author Echo009
 * @since 2021/12/14
 */
@Component
@Slf4j
public class NestedWorkflowNodeValidator implements NodeValidator {

    @Resource
    private WorkflowInfoRepository workflowInfoRepository;


    @Override
    public void validate(PEWorkflowDAG.Node node, WorkflowDAG dag) {
        // 判断对应工作流是否存在
        WorkflowInfoDO workflowInfo = workflowInfoRepository.findById(node.getWfId())
                .orElseThrow(() -> new PowerJobException("Illegal nested workflow node,specified workflow is not exist,node name : " + node.getNodeName()));
        if (workflowInfo.getStatus() == SwitchableStatus.DELETED.getV()) {
            throw new PowerJobException("Illegal nested workflow node,specified workflow has been deleted,node name : " + node.getNodeName());
        }
        // 不允许多层嵌套，即 嵌套工作流节点引用的工作流中不能包含嵌套节点
        PEWorkflowDAG peDag = JSON.parseObject(workflowInfo.getPeDAG(), PEWorkflowDAG.class);
        for (PEWorkflowDAG.Node peDagNode : peDag.getNodes()) {
            if (Objects.equals(peDagNode.getNodeType(), WorkflowNodeType.NESTED_WORKFLOW.getCode())) {
                throw new PowerJobException("Illegal nested workflow node,specified workflow must be a simple workflow,node name : " + node.getNodeName());
            }
        }
    }

    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.NESTED_WORKFLOW;
    }
}
