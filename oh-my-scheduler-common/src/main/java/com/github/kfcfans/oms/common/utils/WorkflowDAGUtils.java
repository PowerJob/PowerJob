package com.github.kfcfans.oms.common.utils;

import com.github.kfcfans.oms.common.OmsException;
import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import com.github.kfcfans.oms.common.model.WorkflowDAG;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * DAG 工具类
 *
 * @author tjq
 * @since 2020/5/26
 */
public class WorkflowDAGUtils {

    /**
     * 将点线表示法的DAG图转化为引用表达法的DAG图
     * @param PEWorkflowDAG 点线表示法的DAG图
     * @return 引用表示法的DAG图
     */
    public static WorkflowDAG convert(PEWorkflowDAG PEWorkflowDAG) {
        Set<Long> rootIds = Sets.newHashSet();
        Map<Long, WorkflowDAG.Node> id2Node = Maps.newHashMap();

        if (PEWorkflowDAG.getNodes() == null || PEWorkflowDAG.getNodes().isEmpty()) {
            throw new OmsException("empty graph");
        }

        // 创建节点
        PEWorkflowDAG.getNodes().forEach(node -> {
            Long jobId = node.getJobId();
            WorkflowDAG.Node n = new WorkflowDAG.Node(Lists.newLinkedList(), jobId, node.getJobName(), null, false, null);
            id2Node.put(jobId, n);

            // 初始阶段，每一个点都设为顶点
            rootIds.add(jobId);
        });

        // 连接图像
        PEWorkflowDAG.getEdges().forEach(edge -> {
            WorkflowDAG.Node from = id2Node.get(edge.getFrom());
            WorkflowDAG.Node to = id2Node.get(edge.getTo());

            if (from == null || to == null) {
                throw new OmsException("Illegal Edge: " + JsonUtils.toJSONString(edge));
            }

            from.getSuccessors().add(to);

            // 被连接的点不可能成为 root，移除
            rootIds.remove(to.getJobId());
        });

        // 合法性校验
        if (rootIds.size() != 1) {
            throw new OmsException("Illegal DAG Graph: " + JsonUtils.toJSONString(PEWorkflowDAG));
        }

        return new WorkflowDAG(id2Node.get(rootIds.iterator().next()));
    }

    /**
     * 校验 DAG 是否有效
     * @param peWorkflowDAG 点线表示法的 DAG 图
     * @return true/false
     */
    public static boolean valid(PEWorkflowDAG peWorkflowDAG) {
        try {
            WorkflowDAG workflowDAG = convert(peWorkflowDAG);
            return check(workflowDAG.getRoot(), Sets.newHashSet());
        }catch (Exception ignore) {
        }
        return false;
    }

    private static boolean check(WorkflowDAG.Node root, Set<Long> ids) {

        // 递归出口（出现之前的节点则代表有环，失败；出现无后继者节点，则说明该路径成功）
        if (ids.contains(root.getJobId())) {
            return false;
        }
        if (root.getSuccessors().isEmpty()) {
            return true;
        }
        ids.add(root.getJobId());
        for (WorkflowDAG.Node node : root.getSuccessors()) {
            if (!check(node, Sets.newHashSet(ids))) {
                return false;
            }
        }
        return true;
    }
}
