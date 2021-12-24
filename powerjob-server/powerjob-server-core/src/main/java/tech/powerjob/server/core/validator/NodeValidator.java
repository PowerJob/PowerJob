package tech.powerjob.server.core.validator;

import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;

/**
 * @author Echo009
 * @since 2021/12/14
 */
public interface NodeValidator {
    /**
     * 校验工作流节点是否合法
     * @param node 节点
     * @param dag  dag
     */
    void validate(PEWorkflowDAG.Node node, WorkflowDAG dag);

    /**
     * 匹配的节点类型
     * @return node type
     */
    WorkflowNodeType matchingType();

}
