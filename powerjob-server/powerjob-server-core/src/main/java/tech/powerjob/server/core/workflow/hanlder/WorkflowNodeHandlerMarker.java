package tech.powerjob.server.core.workflow.hanlder;

import tech.powerjob.common.enums.WorkflowNodeType;

/**
 * @author Echo009
 * @since 2021/12/9
 */
public interface WorkflowNodeHandlerMarker {


    /**
     * 返回能够处理的节点类型
     * @return matching node type
     */
    WorkflowNodeType matchingType();



}
