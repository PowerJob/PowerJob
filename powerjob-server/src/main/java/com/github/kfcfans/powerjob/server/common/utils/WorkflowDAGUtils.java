package com.github.kfcfans.powerjob.server.common.utils;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.server.model.WorkflowDAG;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAG 工具类
 *
 * @author tjq
 * @since 2020/5/26
 */
public class WorkflowDAGUtils {

    private WorkflowDAGUtils() {

    }
    /**
     * 获取所有根节点
     *
     * @param peWorkflowDAG 点线表示法的DAG图
     * @return 根节点列表
     */
    public static List<PEWorkflowDAG.Node> listRoots(PEWorkflowDAG peWorkflowDAG) {

        Map<Long, PEWorkflowDAG.Node> nodeId2Node = Maps.newHashMap();
        peWorkflowDAG.getNodes().forEach(node -> nodeId2Node.put(node.getNodeId(), node));
        peWorkflowDAG.getEdges().forEach(edge -> nodeId2Node.remove(edge.getTo()));

        return Lists.newLinkedList(nodeId2Node.values());
    }

    /**
     * 校验 DAG 是否有效
     *
     * @param peWorkflowDAG 点线表示法的 DAG 图
     * @return true/false
     */
    public static boolean valid(PEWorkflowDAG peWorkflowDAG) {

        // 节点ID
        Set<Long> nodeIds = Sets.newHashSet();
        for (PEWorkflowDAG.Node n : peWorkflowDAG.getNodes()) {
            if (nodeIds.contains(n.getNodeId())) {
                return false;
            }
            nodeIds.add(n.getNodeId());
        }

        try {
            WorkflowDAG workflowDAG = convert(peWorkflowDAG);

            // 检查所有顶点的路径
            for (WorkflowDAG.Node root : workflowDAG.getRoots()) {
                if (invalidPath(root, Sets.newHashSet())) {
                    return false;
                }
            }
            return true;
        } catch (Exception ignore) {
            // ignore
        }
        return false;
    }

    /**
     * 将点线表示法的DAG图转化为引用表达法的DAG图
     *
     * @param peWorkflowDAG 点线表示法的DAG图
     * @return 引用表示法的DAG图
     */
    public static WorkflowDAG convert(PEWorkflowDAG peWorkflowDAG) {
        Set<Long> rootIds = Sets.newHashSet();
        Map<Long, WorkflowDAG.Node> id2Node = Maps.newHashMap();

        if (peWorkflowDAG.getNodes() == null || peWorkflowDAG.getNodes().isEmpty()) {
            throw new PowerJobException("empty graph");
        }

        // 创建节点
        peWorkflowDAG.getNodes().forEach(node -> {
            Long nodeId = node.getNodeId();
            WorkflowDAG.Node n = new WorkflowDAG.Node(Lists.newLinkedList(), node.getNodeId(), node.getJobId(), node.getJobName(), InstanceStatus.WAITING_DISPATCH.getV());
            id2Node.put(nodeId, n);

            // 初始阶段，每一个点都设为顶点
            rootIds.add(nodeId);
        });

        // 连接图像
        peWorkflowDAG.getEdges().forEach(edge -> {
            WorkflowDAG.Node from = id2Node.get(edge.getFrom());
            WorkflowDAG.Node to = id2Node.get(edge.getTo());

            if (from == null || to == null) {
                throw new PowerJobException("Illegal Edge: " + JsonUtils.toJSONString(edge));
            }

            from.getSuccessors().add(to);

            // 被连接的点不可能成为 root，移除
            rootIds.remove(to.getNodeId());
        });

        // 合法性校验（至少存在一个顶点）
        if (rootIds.isEmpty()) {
            throw new PowerJobException("Illegal DAG: " + JsonUtils.toJSONString(peWorkflowDAG));
        }

        List<WorkflowDAG.Node> roots = Lists.newLinkedList();
        rootIds.forEach(id -> roots.add(id2Node.get(id)));
        return new WorkflowDAG(roots);
    }


    private static boolean invalidPath(WorkflowDAG.Node root, Set<Long> ids) {

        // 递归出口（出现之前的节点则代表有环，失败；出现无后继者节点，则说明该路径成功）
        if (ids.contains(root.getNodeId())) {
            return true;
        }
        if (root.getSuccessors().isEmpty()) {
            return false;
        }
        ids.add(root.getNodeId());
        for (WorkflowDAG.Node node : root.getSuccessors()) {
            if (invalidPath(node, Sets.newHashSet(ids))) {
                return true;
            }
        }
        return false;
    }
}
