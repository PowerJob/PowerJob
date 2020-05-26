package com.github.kfcfans.oms.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DAG 工作流对象
 * 使用引用，易于计算
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDAG {

    private Node root;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Node {
        // 后继者，子节点
        private List<Node> successors;
        private Long jobId;
        private String jobName;
    }
}
