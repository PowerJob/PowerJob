package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点类型
 *
 * @author Echo009
 * @since 2021/3/7
 */
@Getter
@AllArgsConstructor
public enum WorkflowNodeType {
    /**
     * 任务节点
     */
    JOB(1,false),
    /**
     * 判断节点
     */
    DECISION(2,true),
    /**
     * 内嵌工作流
     */
    NESTED_WORKFLOW(3,false),

    ;

    private final int code;
    /**
     * 控制节点
     */
    private final boolean controlNode;

    public static WorkflowNodeType of(int code) {
        for (WorkflowNodeType nodeType : values()) {
            if (nodeType.code == code) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("unknown WorkflowNodeType of " + code);
    }



}
