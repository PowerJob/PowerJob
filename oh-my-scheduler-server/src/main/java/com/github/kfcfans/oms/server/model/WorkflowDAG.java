package com.github.kfcfans.oms.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DAG 工作流对象
 * 使用引用，易于计算（不再参与主运算，起辅助作用）
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDAG {

    // DAG允许存在多个顶点
    private List<Node> roots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Node {
        // 后继者，子节点
        private List<Node> successors;
        private Long jobId;
        private String jobName;

        // 运行时参数
        private Long instanceId; // 任务实例ID
        private int status; // 状态 WAITING_DISPATCH -> RUNNING -> SUCCEED/FAILED/STOPPED
        private String result;
    }
}
