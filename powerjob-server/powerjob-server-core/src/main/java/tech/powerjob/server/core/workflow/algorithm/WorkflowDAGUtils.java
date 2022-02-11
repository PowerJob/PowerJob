package tech.powerjob.server.core.workflow.algorithm;

import com.google.common.collect.*;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.serialize.JsonUtils;

import java.util.*;

/**
 * DAG 工具类
 *
 * @author tjq
 * @author Echo009
 * @since 2020/5/26
 */
public class WorkflowDAGUtils {

    private WorkflowDAGUtils() {

    }

    /**
     * 重置可重试节点的状态信息
     * @param dag 合法的有向无环图
     */
    public static void resetRetryableNode(PEWorkflowDAG dag){
        // 将需要重试的节点状态重置（失败且不允许跳过的 或者 手动终止的）
        for (PEWorkflowDAG.Node node : dag.getNodes()) {
            boolean realFailed = node.getStatus() == InstanceStatus.FAILED.getV() && isNotAllowSkipWhenFailed(node);
            if (realFailed || node.getStatus() == InstanceStatus.STOPPED.getV()) {
                node.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
                // 仅重置任务节点的实例 id 信息
                if (node.getNodeType() == null || node.getNodeType() == WorkflowNodeType.JOB.getCode()){
                    node.setInstanceId(null);
                }
            }
        }
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

        // 校验节点 ID 是否重复
        Set<Long> nodeIds = Sets.newHashSet();
        // 不允许为空
        if (peWorkflowDAG.getNodes() == null || peWorkflowDAG.getNodes().isEmpty()){
            return false;
        }
        for (PEWorkflowDAG.Node n : peWorkflowDAG.getNodes()) {
            if (nodeIds.contains(n.getNodeId())) {
                return false;
            }
            nodeIds.add(n.getNodeId());
        }

        try {
            // 记录遍历过的所有节点 ID
            HashSet<Long> traversalNodeIds = Sets.newHashSet();
            WorkflowDAG dag = convert(peWorkflowDAG);
            // 检查所有顶点的路径
            for (WorkflowDAG.Node root : dag.getRoots()) {
                if (invalidPath(root, Sets.newHashSet(), traversalNodeIds)) {
                    return false;
                }
            }
            // 理论上应该遍历过图中的所有节点，如果不相等则说明有环 (孤立的环)
            return traversalNodeIds.size() == nodeIds.size();

        } catch (Exception ignore) {
            // ignore
        }
        return false;
    }


    /**
     * Add by Echo009 on 2021/02/08
     * 获取准备好的节点（非完成状态的节点且，前置依赖节点为空或者均处于完成状态）
     * 注意，这里会直接将当前 disable（enable = false）的节点的状态置为完成
     *
     * @param dag 点线表示法的DAG图
     * @return 当前可执行的节点
     */
    public static List<PEWorkflowDAG.Node> listReadyNodes(PEWorkflowDAG dag) {
        // 保存 nodeId -> Node 的映射关系
        Map<Long, PEWorkflowDAG.Node> nodeId2Node = Maps.newHashMap();

        List<PEWorkflowDAG.Node> dagNodes = dag.getNodes();
        for (PEWorkflowDAG.Node node : dagNodes) {
            nodeId2Node.put(node.getNodeId(), node);
        }
        // 构建依赖树（下游任务需要哪些上游任务完成才能执行）
        Multimap<Long, Long> relyMap = LinkedListMultimap.create();
        // 后继节点 Map
        Multimap<Long, Long> successorMap = LinkedListMultimap.create();
        dag.getEdges().forEach(edge -> {
            relyMap.put(edge.getTo(), edge.getFrom());
            successorMap.put(edge.getFrom(), edge.getTo());
        });
        List<PEWorkflowDAG.Node> readyNodes = Lists.newArrayList();
        List<PEWorkflowDAG.Node> skipNodes = Lists.newArrayList();

        for (PEWorkflowDAG.Node currentNode : dagNodes) {
            if (!isReadyNode(currentNode.getNodeId(), nodeId2Node, relyMap)) {
                continue;
            }
            // 需要直接跳过的节点
            if (currentNode.getEnable() != null && !currentNode.getEnable()) {
                skipNodes.add(currentNode);
            } else {
                readyNodes.add(currentNode);
            }
        }
        // 当前直接跳过的节点不为空
        if (!skipNodes.isEmpty()) {
            for (PEWorkflowDAG.Node skipNode : skipNodes) {
                // move
                readyNodes.addAll(moveAndObtainReadySuccessor(skipNode, nodeId2Node, relyMap, successorMap));
            }
        }
        return readyNodes;
    }

    /**
     * 移动并获取就绪的后继节点
     *
     * @param skippedNode  当前需要跳过的节点
     * @param nodeId2Node  nodeId -> Node
     * @param relyMap      to-node id  -> list of from-node id
     * @param successorMap from-node id -> list of to-node id
     * @return 就绪的后继节点
     */
    private static List<PEWorkflowDAG.Node> moveAndObtainReadySuccessor(PEWorkflowDAG.Node skippedNode, Map<Long, PEWorkflowDAG.Node> nodeId2Node, Multimap<Long, Long> relyMap, Multimap<Long, Long> successorMap) {

        // 更新当前跳过节点的状态
        skippedNode.setStatus(InstanceStatus.SUCCEED.getV());
        skippedNode.setResult(SystemInstanceResult.DISABLE_NODE);
        // 有可能出现需要连续移动的情况
        List<PEWorkflowDAG.Node> readyNodes = Lists.newArrayList();
        List<PEWorkflowDAG.Node> skipNodes = Lists.newArrayList();
        // 获取当前跳过节点的后继节点
        Collection<Long> successors = successorMap.get(skippedNode.getNodeId());
        for (Long successor : successors) {
            // 判断后继节点是否处于 Ready 状态（前驱节点均处于完成状态）
            if (isReadyNode(successor, nodeId2Node, relyMap)) {
                PEWorkflowDAG.Node node = nodeId2Node.get(successor);
                if (node.getEnable() != null && !node.getEnable()) {
                    // 需要跳过
                    skipNodes.add(node);
                    continue;
                }
                readyNodes.add(node);
            }
        }
        // 深度优先，继续移动
        if (!skipNodes.isEmpty()) {
            for (PEWorkflowDAG.Node node : skipNodes) {
                readyNodes.addAll(moveAndObtainReadySuccessor(node, nodeId2Node, relyMap, successorMap));
            }
        }
        return readyNodes;
    }

    /**
     * 判断当前节点是否准备就绪
     *
     * @param nodeId      Node id
     * @param nodeId2Node Node id -> Node
     * @param relyMap     to-node id  -> list of from-node id
     * @return true if current node is ready
     */
    private static boolean isReadyNode(long nodeId, Map<Long, PEWorkflowDAG.Node> nodeId2Node, Multimap<Long, Long> relyMap) {
        PEWorkflowDAG.Node currentNode = nodeId2Node.get(nodeId);
        int currentNodeStatus = currentNode.getStatus() == null ? InstanceStatus.WAITING_DISPATCH.getV() : currentNode.getStatus();
        // 跳过已完成节点（处理成功 或者 处理失败）和已派发节点（ 状态为运行中 ）
        if (InstanceStatus.FINISHED_STATUS.contains(currentNodeStatus)
                || currentNodeStatus == InstanceStatus.RUNNING.getV()) {
            return false;
        }
        Collection<Long> relyNodeIds = relyMap.get(nodeId);
        for (Long relyNodeId : relyNodeIds) {
            PEWorkflowDAG.Node relyNode = nodeId2Node.get(relyNodeId);
            int relyNodeStatus = relyNode.getStatus() == null ? InstanceStatus.WAITING_DISPATCH.getV() : relyNode.getStatus();
            // 只要依赖的节点有一个未完成，那么就不是就绪状态
            // 注意，这里允许失败的原因是有允许失败跳过节点的存在，对于不允许跳过的失败节点，一定走不到这里（工作流会被打断）
            if (InstanceStatus.GENERALIZED_RUNNING_STATUS.contains(relyNodeStatus)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotAllowSkipWhenFailed(PEWorkflowDAG.Node node) {
        // 默认不允许跳过
        return node.getSkipWhenFailed() == null || !node.getSkipWhenFailed();
    }

    /**
     * 处理被 disable 掉的边
     * 1. 将仅能通过被 disable 掉的边可达的节点标记为 disable（disableByControlNode），将状态更新为已取消
     * 2. 将这些被 disable 掉的节点的出口边都标记为 disable
     * 3. 递归调用自身，继续处理被 disable 的边
     */
    @SuppressWarnings("squid:S3776")
    public static void handleDisableEdges(List<PEWorkflowDAG.Edge> disableEdges, WorkflowDAG dag) {
        if (disableEdges.isEmpty()) {
            return;
        }
        List<PEWorkflowDAG.Node> disableNodes = Lists.newArrayList();
        // 处理边上的节点，如果该节点仅能通过被 disable 掉的边可达，那么将该节点标记为 disable ，disableByControlNode ，并且将状态更新为 已取消
        for (PEWorkflowDAG.Edge disableEdge : disableEdges) {
            WorkflowDAG.Node toNode = dag.getNode(disableEdge.getTo());
            // 判断是否仅能通过被 disable 掉的边可达
            Collection<PEWorkflowDAG.Edge> dependenceEdges = toNode.getDependenceEdgeMap().values();
            boolean shouldBeDisable = true;
            for (PEWorkflowDAG.Edge dependenceEdge : dependenceEdges) {
                if (dependenceEdge.getEnable() == null || dependenceEdge.getEnable()) {
                    shouldBeDisable = false;
                    break;
                }
            }
            if (shouldBeDisable) {
                // disable
                PEWorkflowDAG.Node node = toNode.getHolder();
                node.setEnable(false)
                        .setDisableByControlNode(true)
                        .setStatus(InstanceStatus.CANCELED.getV());
                disableNodes.add(node);
            }
        }
        if (!disableNodes.isEmpty()) {
            // 被 disable 掉的节点的出口边都会被标记为 disable
            List<PEWorkflowDAG.Edge> targetEdges = Lists.newArrayList();
            for (PEWorkflowDAG.Node disableNode : disableNodes) {
                WorkflowDAG.Node node = dag.getNode(disableNode.getNodeId());
                Collection<PEWorkflowDAG.Edge> edges = node.getSuccessorEdgeMap().values();
                for (PEWorkflowDAG.Edge edge : edges) {
                    edge.setEnable(false);
                    targetEdges.add(edge);
                }
            }
            // 广度优先 继续处理被 disable 掉的边
            handleDisableEdges(targetEdges, dag);
        }
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
            WorkflowDAG.Node n = new WorkflowDAG.Node(node);
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
            from.getSuccessorEdgeMap().put(to, edge);
            to.getDependencies().add(from);
            to.getDependenceEdgeMap().put(from, edge);
            // 被连接的点不可能成为 root，移除
            rootIds.remove(to.getNodeId());
        });

        // 合法性校验（至少存在一个顶点）
        if (rootIds.isEmpty()) {
            throw new PowerJobException("Illegal DAG: " + JsonUtils.toJSONString(peWorkflowDAG));
        }

        List<WorkflowDAG.Node> roots = Lists.newLinkedList();
        rootIds.forEach(id -> roots.add(id2Node.get(id)));
        return new WorkflowDAG(roots, id2Node);
    }


    private static boolean invalidPath(WorkflowDAG.Node root, Set<Long> ids, Set<Long> nodeIdContainer) {

        // 递归出口（出现之前的节点则代表有环，失败；出现无后继者节点，则说明该路径成功）
        if (ids.contains(root.getNodeId())) {
            return true;
        }
        nodeIdContainer.add(root.getNodeId());
        if (root.getSuccessors().isEmpty()) {
            return false;
        }
        ids.add(root.getNodeId());
        for (WorkflowDAG.Node node : root.getSuccessors()) {
            if (invalidPath(node, Sets.newHashSet(ids), nodeIdContainer)) {
                return true;
            }
        }
        return false;
    }
}
