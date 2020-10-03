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

    /**
     * 获取所有根节点
     * @param peWorkflowDAG 点线表示法的DAG图
     * @return 根节点列表
     */
    public static List<PEWorkflowDAG.Node> listRoots(PEWorkflowDAG peWorkflowDAG) {

        Map<Long, PEWorkflowDAG.Node> jobId2Node = Maps.newHashMap();
        peWorkflowDAG.getNodes().forEach(node -> jobId2Node.put(node.getJobId(), node));
        peWorkflowDAG.getEdges().forEach(edge -> jobId2Node.remove(edge.getTo()));

        return Lists.newLinkedList(jobId2Node.values());
    }

    /**
     * 校验 DAG 是否有效
     * @param peWorkflowDAG 点线表示法的 DAG 图
     * @return true/false
     */
    public static boolean valid(PEWorkflowDAG peWorkflowDAG) {

        // 点不允许重复，一个工作流中某个任务只允许出现一次
        Set<Long> jobIds = Sets.newHashSet();
        for (PEWorkflowDAG.Node n : peWorkflowDAG.getNodes()) {
            if (jobIds.contains(n.getJobId())) {
                return false;
            }
            jobIds.add(n.getJobId());
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
        }catch (Exception ignore) {
        }
        return false;
    }

    /**
     * 将点线表示法的DAG图转化为引用表达法的DAG图
     * @param PEWorkflowDAG 点线表示法的DAG图
     * @return 引用表示法的DAG图
     */
    public static WorkflowDAG convert(PEWorkflowDAG PEWorkflowDAG) {
        Set<Long> rootIds = Sets.newHashSet();
        Map<Long, WorkflowDAG.Node> id2Node = Maps.newHashMap();

        if (PEWorkflowDAG.getNodes() == null || PEWorkflowDAG.getNodes().isEmpty()) {
            throw new PowerJobException("empty graph");
        }

        // 创建节点
        PEWorkflowDAG.getNodes().forEach(node -> {
            Long jobId = node.getJobId();
            WorkflowDAG.Node n = new WorkflowDAG.Node(Lists.newLinkedList(), jobId, node.getJobName(), null, InstanceStatus.WAITING_DISPATCH.getV(), null);
            id2Node.put(jobId, n);

            // 初始阶段，每一个点都设为顶点
            rootIds.add(jobId);
        });

        // 连接图像
        PEWorkflowDAG.getEdges().forEach(edge -> {
            WorkflowDAG.Node from = id2Node.get(edge.getFrom());
            WorkflowDAG.Node to = id2Node.get(edge.getTo());

            if (from == null || to == null) {
                throw new PowerJobException("Illegal Edge: " + JsonUtils.toJSONString(edge));
            }

            from.getSuccessors().add(to);

            // 被连接的点不可能成为 root，移除
            rootIds.remove(to.getJobId());
        });

        // 合法性校验（至少存在一个顶点）
        if (rootIds.size() < 1) {
            throw new PowerJobException("Illegal DAG: " + JsonUtils.toJSONString(PEWorkflowDAG));
        }

        List<WorkflowDAG.Node> roots = Lists.newLinkedList();
        rootIds.forEach(id -> roots.add(id2Node.get(id)));
        return new WorkflowDAG(roots);
    }


    private static boolean invalidPath(WorkflowDAG.Node root, Set<Long> ids) {

        // 递归出口（出现之前的节点则代表有环，失败；出现无后继者节点，则说明该路径成功）
        if (ids.contains(root.getJobId())) {
            return true;
        }
        if (root.getSuccessors().isEmpty()) {
            return false;
        }
        ids.add(root.getJobId());
        for (WorkflowDAG.Node node : root.getSuccessors()) {
            if (invalidPath(node, Sets.newHashSet(ids))) {
                return true;
            }
        }
        return false;
    }
}
