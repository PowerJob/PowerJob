package tech.powerjob.server.core.workflow.hanlder;

import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;

/**
 * @author Echo009
 * @since 2021/12/9
 */
public interface TaskNodeHandler extends WorkflowNodeHandlerMarker {

    /**
     * 创建任务实例
     *
     * @param node           目标节点
     * @param dag            DAG
     * @param wfInstanceInfo 工作流实例
     */
    void createTaskInstance(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo);

    /**
     * 执行任务实例
     *
     * @param node 目标节点
     */
    void startTaskInstance(PEWorkflowDAG.Node node);


}
