package tech.powerjob.server.core.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.core.workflow.hanlder.ControlNodeHandler;
import tech.powerjob.server.core.workflow.hanlder.TaskNodeHandler;
import tech.powerjob.server.core.workflow.hanlder.WorkflowNodeHandlerMarker;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInstanceInfoRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author Echo009
 * @since 2021/12/9
 */
@Slf4j
@Service
public class WorkflowNodeHandleService {

    private final Map<WorkflowNodeType, ControlNodeHandler> controlNodeHandlerContainer;

    private final Map<WorkflowNodeType, TaskNodeHandler> taskNodeHandlerContainer;

    private final WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    public WorkflowNodeHandleService(List<ControlNodeHandler> controlNodeHandlerList, List<TaskNodeHandler> taskNodeHandlerList, WorkflowInstanceInfoRepository workflowInstanceInfoRepository) {
        // init
        controlNodeHandlerContainer = new EnumMap<>(WorkflowNodeType.class);
        taskNodeHandlerContainer = new EnumMap<>(WorkflowNodeType.class);
        controlNodeHandlerList.forEach(controlNodeHandler -> controlNodeHandlerContainer.put(controlNodeHandler.matchingType(), controlNodeHandler));
        taskNodeHandlerList.forEach(taskNodeHandler -> taskNodeHandlerContainer.put(taskNodeHandler.matchingType(), taskNodeHandler));
        //
        this.workflowInstanceInfoRepository = workflowInstanceInfoRepository;
    }

    /**
     * 处理任务节点
     * 注意，上层调用方必须保证这里的 taskNodeList 不能为空
     */
    public void handleTaskNodes(List<PEWorkflowDAG.Node> taskNodeList, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {

        // 创建任务实例
        taskNodeList.forEach(taskNode -> {
            // 注意：这里必须保证任务实例全部创建成功，如果在这里创建实例部分失败，会导致 DAG 信息不会更新，已经生成的实例节点在工作流日志中没法展示
            TaskNodeHandler taskNodeHandler = (TaskNodeHandler) findMatchingHandler(taskNode);
            taskNodeHandler.createTaskInstance(taskNode, dag, wfInstanceInfo);
            log.debug("[Workflow-{}|{}] workflowInstance start to process new node(nodeId={},jobId={})", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), taskNode.getNodeId(), taskNode.getJobId());
        });
        // 持久化工作流实例信息
        wfInstanceInfo.setDag(JSON.toJSONString(dag));
        workflowInstanceInfoRepository.saveAndFlush(wfInstanceInfo);
        // 启动
        taskNodeList.forEach(taskNode -> {
            TaskNodeHandler taskNodeHandler = (TaskNodeHandler) findMatchingHandler(taskNode);
            taskNodeHandler.startTaskInstance(taskNode);
        });


    }

    /**
     * 处理控制节点
     * 注意，上层调用方必须保证这里的 controlNodeList 不能为空
     */
    public void handleControlNodes(List<PEWorkflowDAG.Node> controlNodeList, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {
        for (PEWorkflowDAG.Node node : controlNodeList) {
            handleControlNode(node, dag, wfInstanceInfo);
        }
    }

    public void handleControlNode(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {
        ControlNodeHandler controlNodeHandler = (ControlNodeHandler) findMatchingHandler(node);
        node.setStartTime(CommonUtils.formatTime(System.currentTimeMillis()));
        controlNodeHandler.handle(node, dag, wfInstanceInfo);
        node.setFinishedTime(CommonUtils.formatTime(System.currentTimeMillis()));
    }


    private WorkflowNodeHandlerMarker findMatchingHandler(PEWorkflowDAG.Node node) {
        WorkflowNodeType nodeType = WorkflowNodeType.of(node.getNodeType());
        WorkflowNodeHandlerMarker res;
        if (!nodeType.isControlNode()) {
            res = taskNodeHandlerContainer.get(nodeType);
        } else {
            res = controlNodeHandlerContainer.get(nodeType);
        }
        if (res == null) {
            // impossible
            throw new UnsupportedOperationException("unsupported node type : " + nodeType);
        }
        return res;
    }

}
