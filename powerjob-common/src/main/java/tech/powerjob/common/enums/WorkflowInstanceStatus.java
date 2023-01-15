package tech.powerjob.common.enums;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Workflow 任务运行状态
 *
 * @author tjq
 * @since 2020/5/26
 */
@Getter
@AllArgsConstructor
public enum WorkflowInstanceStatus {
    /**
     * 初始状态为等待调度
     */
    WAITING(1, "等待调度"),
    RUNNING(2, "运行中"),
    FAILED(3, "失败"),
    SUCCEED(4, "成功"),
    STOPPED(10, "手动停止");

    /**
     * 广义的运行状态
     */
    public static final List<Integer> GENERALIZED_RUNNING_STATUS = Collections.unmodifiableList(Lists.newArrayList(WAITING.v, RUNNING.v));
    /**
     * 结束状态
     */
    public static final List<Integer> FINISHED_STATUS = Collections.unmodifiableList(Lists.newArrayList(FAILED.v, SUCCEED.v, STOPPED.v));

    private final int v;

    private final String des;

    public static WorkflowInstanceStatus of(int v) {
        for (WorkflowInstanceStatus is : values()) {
            if (v == is.v) {
                return is;
            }
        }
        throw new IllegalArgumentException("WorkflowInstanceStatus has no item for value " + v);
    }
}
