package com.github.kfcfans.oms.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Point & Line DAG 表示法
 * 点 + 线，易于表达和传播
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PLWorkflowDAG {

    // DAG 图（点线表示法）
    private List<Node> nodes;
    private List<Edge> edges;

    // 点
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private Long jobId;
        private String jobName;
    }

    // 边 jobId -> jobId
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge {
        private Long from;
        private Long to;
    }
}
