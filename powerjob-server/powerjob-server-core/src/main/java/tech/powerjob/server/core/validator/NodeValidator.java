package tech.powerjob.server.core.validator;

import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;

/**
 * @author Echo009
 * @since 2021/12/14
 */
public interface NodeValidator {
    /**
     * 校验工作流节点（校验拓扑关系等）
     * @param node 节点
     * @param dag  dag
     */
    void complexValidate(WorkflowNodeInfoDO node, WorkflowDAG dag);

    /**
     * 校验工作流节点
     * @param node 节点
     */
    void simpleValidate(WorkflowNodeInfoDO node);

    /**
     * 匹配的节点类型
     * @return node type
     */
    WorkflowNodeType matchingType();

}
