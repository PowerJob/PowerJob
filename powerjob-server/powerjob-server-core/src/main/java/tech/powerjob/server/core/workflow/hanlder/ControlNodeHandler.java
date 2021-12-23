package tech.powerjob.server.core.workflow.hanlder;

import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;

/**
 * @author Echo009
 * @since 2021/12/9
 */
public interface ControlNodeHandler extends WorkflowNodeHandlerMarker {

    /**
     * 处理控制节点
     *
     * @param node           需要被处理的目标节点
     * @param dag            节点所属 DAG
     * @param wfInstanceInfo 节点所属工作流实例
     */
    void handle(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo);


}
