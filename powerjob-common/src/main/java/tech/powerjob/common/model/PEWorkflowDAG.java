package tech.powerjob.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tech.powerjob.common.enums.WorkflowNodeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;

/**
 * Points & edges for DAG, making it easier to describe or transfer.
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@NoArgsConstructor
public class PEWorkflowDAG implements Serializable {

    /**
     * Nodes of DAG diagram.
     */
    private List<Node> nodes;
    /**
     * Edges of DAG diagram.
     */
    private List<Edge> edges;

    /**
     * Point.
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class Node implements Serializable {
        /**
         * node id
         *
         * @since 20210128
         */
        private Long nodeId;
        /* Instance running param, which is not required by DAG. */

        /**
         * note type
         *
         * @see WorkflowNodeType
         * @since 20210316
         */
        private Integer nodeType;
        /**
         * job id or workflow id (if this Node type is a nested workflow)
         *
         * @see WorkflowNodeType#NESTED_WORKFLOW
         */
        private Long jobId;
        /**
         * node name
         */
        private String nodeName;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long instanceId;
        /**
         * for decision node, it is JavaScript code
         */
        private String nodeParams;

        private Integer status;
        /**
         * for decision node, it only be can "true" or "false"
         */
        private String result;
        /**
         * instanceId will be null if disable .
         */
        private Boolean enable;
        /**
         * mark node which disable by control node.
         */
        private Boolean disableByControlNode;

        private Boolean skipWhenFailed;

        private String startTime;

        private String finishedTime;

        public Node(Long nodeId) {
            this.nodeId = nodeId;
            this.nodeType = WorkflowNodeType.JOB.getCode();
        }

        public Node(Long nodeId, Integer nodeType) {
            this.nodeId = nodeId;
            this.nodeType = nodeType;
        }
    }

    /**
     * Edge formed by two node ids.
     */
    @Data
    @NoArgsConstructor
    public static class Edge implements Serializable {

        private Long from;

        private Long to;
        /**
         * property,support for complex flow control
         * for decision node , it can be "true" or "false"
         */
        private String property;

        private Boolean enable;

        public Edge(long from, long to) {
            this.from = from;
            this.to = to;
        }

        public Edge(long from, long to, String property) {
            this.from = from;
            this.to = to;
            this.property = property;
        }
    }

    public PEWorkflowDAG(@Nonnull List<Node> nodes, @Nullable List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges == null ? Lists.newLinkedList() : edges;
    }
}
