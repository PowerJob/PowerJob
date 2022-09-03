package tech.powerjob.server.core.workflow.algorithm;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.*;
import tech.powerjob.common.model.PEWorkflowDAG;

import java.util.List;
import java.util.Map;

/**
 * DAG 工作流对象
 * 节点中均记录了上游以及下游的连接关系(无法使用 JSON 来序列化以及反序列化)
 *
 * @author tjq
 * @author Echo009
 * @since 2020/5/26
 */
@Data
@ToString(exclude = {"nodeMap"})
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDAG {

    /**
     * DAG允许存在多个顶点
     */
    private List<Node> roots;

    private Map<Long, Node> nodeMap;

    public Node getNode(Long nodeId) {
        if (nodeMap == null) {
            return null;
        }
        return nodeMap.get(nodeId);
    }

    @Getter
    @Setter
    @EqualsAndHashCode(exclude = {"dependencies", "dependenceEdgeMap", "successorEdgeMap", "holder","successors"})
    @ToString(exclude = {"dependencies", "dependenceEdgeMap", "successorEdgeMap", "holder"})
    @NoArgsConstructor
    public static final class Node {

        public Node(PEWorkflowDAG.Node node) {
            this.nodeId = node.getNodeId();
            this.holder = node;
            this.dependencies = Lists.newLinkedList();
            this.dependenceEdgeMap = Maps.newHashMap();
            this.successors = Lists.newLinkedList();
            this.successorEdgeMap = Maps.newHashMap();
        }

        /**
         * node id
         *
         * @since 20210128
         */
        private Long nodeId;

        private PEWorkflowDAG.Node holder;
        /**
         * 依赖的上游节点
         */
        private List<Node> dependencies;
        /**
         * 连接依赖节点的边
         */
        private Map<Node, PEWorkflowDAG.Edge> dependenceEdgeMap;
        /**
         * 后继者，子节点
         */
        private List<Node> successors;
        /**
         * 连接后继节点的边
         */
        private Map<Node, PEWorkflowDAG.Edge> successorEdgeMap;

    }
}
