package com.github.kfcfans.powerjob.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node implements Serializable {
        private Long jobId;
        private String jobName;

        /**
         * Instance running param, which is not required by DAG.
         */
        @JsonSerialize(using= ToStringSerializer.class)
        private Long instanceId;
        private Integer status;
        private String result;

        public Node(Long jobId, String jobName) {
            this.jobId = jobId;
            this.jobName = jobName;
        }
    }

    /**
     * Edge formed by two job ids.
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
