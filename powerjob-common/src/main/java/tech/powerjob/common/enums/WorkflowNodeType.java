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
    JOB(1);


    private final int code;

}
