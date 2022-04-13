package tech.powerjob.server.core.workflow.hanlder.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.core.evaluator.GroovyEvaluator;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAGUtils;
import tech.powerjob.server.core.workflow.hanlder.ControlNodeHandler;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;

import java.util.*;

/**
 * @author Echo009
 * @since 2021/12/9
 */
@Slf4j
@Component
public class DecisionNodeHandler implements ControlNodeHandler {

    private final GroovyEvaluator groovyEvaluator = new GroovyEvaluator();

    /**
     * 处理判断节点
     * 1. 执行脚本
     * 2. 根据返回值 disable 掉相应的边以及节点
     */
    @Override
    public void handle(PEWorkflowDAG.Node node, PEWorkflowDAG dag, WorkflowInstanceInfoDO wfInstanceInfo) {
        String script = node.getNodeParams();
        if (StringUtils.isBlank(script)) {
            log.error("[Workflow-{}|{}]decision node's param is blank! nodeId:{}", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId());
            throw new PowerJobException("decision node's param is blank!");
        }
        // wfContext must be a map
        HashMap<String, String> wfContext = JSON.parseObject(wfInstanceInfo.getWfContext(), new TypeReference<HashMap<String, String>>() {
        });
        Object result;
        try {
            result = groovyEvaluator.evaluate(script, wfContext);
        } catch (Exception e) {
            log.error("[Workflow-{}|{}]failed to evaluate decision node,nodeId:{}", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), e);
            throw new PowerJobException("can't evaluate decision node!");
        }
        boolean finalRes;
        if (result instanceof Boolean) {
            finalRes = ((Boolean) result);
        } else if (result instanceof Number) {
            finalRes = ((Number) result).doubleValue() > 0;
        } else {
            log.error("[Workflow-{}|{}]decision node's return value is illegal,nodeId:{},result:{}", wfInstanceInfo.getWorkflowId(), wfInstanceInfo.getWfInstanceId(), node.getNodeId(), JsonUtils.toJSONString(result));
            throw new PowerJobException("decision node's return value is illegal!");
        }
        handleDag(finalRes, node, dag);
    }


    private void handleDag(boolean res, PEWorkflowDAG.Node node, PEWorkflowDAG peDag) {
        // 更新判断节点的状态为成功
        node.setResult(String.valueOf(res));
        node.setStatus(InstanceStatus.SUCCEED.getV());
        WorkflowDAG dag = WorkflowDAGUtils.convert(peDag);
        // 根据节点的计算结果，将相应的边 disable
        WorkflowDAG.Node targetNode = dag.getNode(node.getNodeId());
        Collection<PEWorkflowDAG.Edge> edges = targetNode.getSuccessorEdgeMap().values();
        if (edges.isEmpty()) {
            return;
        }
        List<PEWorkflowDAG.Edge> disableEdges = new ArrayList<>(edges.size());
        for (PEWorkflowDAG.Edge edge : edges) {
            // 这里一定不会出现异常
            boolean property = Boolean.parseBoolean(edge.getProperty());
            if (res != property) {
                // disable
                edge.setEnable(false);
                disableEdges.add(edge);
            }
        }
        WorkflowDAGUtils.handleDisableEdges(disableEdges,dag);
    }





    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.DECISION;
    }
}
