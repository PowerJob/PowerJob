package tech.powerjob.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
    @AllArgsConstructor
    public static class Node implements Serializable {
        /**
         * node id
         * @since 20210128
         */
        private Long nodeId;
        /* Instance running param, which is not required by DAG. */

        /**
         * note type
         * @since 20210316
         */
        private Integer nodeType;
        /**
         * job id
         */
        private Long jobId;
        /**
         * node name
         */
        private String nodeName;

        @JsonSerialize(using= ToStringSerializer.class)
        private Long instanceId;

        private String nodeParams;

        private Integer status;

        private String result;
        /**
         * instanceId will be null if disable .
         */
        private Boolean enable;

        private Boolean skipWhenFailed;

        public Node(Long nodeId) {
            this.nodeId = nodeId;
        }
    }

    /**
     * Edge formed by two node ids.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge implements Serializable {
        private Long from;
        private Long to;
    }

    public PEWorkflowDAG(@Nonnull List<Node> nodes, @Nullable List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges == null ? Lists.newLinkedList() : edges;
    }
}
