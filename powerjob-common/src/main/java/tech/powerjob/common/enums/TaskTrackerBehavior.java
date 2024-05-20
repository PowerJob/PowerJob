package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TaskTracker 行为枚举
 *
 * @author tjq
 * @since 2024/2/24
 */
@Getter
@AllArgsConstructor
public enum TaskTrackerBehavior {

    /**
     * 普通：不特殊处理，参与集群计算，会导致 TaskTracker 负载比常规节点高。适用于节点数不那么多，任务不那么繁重的场景
     */
    NORMAL(1),
    /**
     * 划水：只负责管理节点，不参与计算，稳定性最优。适用于节点数量非常多的大规模计算场景，少一个计算节点来换取稳定性提升
     */
    PADDLING(11)
    ;


    private final Integer v;

    public static TaskTrackerBehavior of(Integer type) {

        if (type == null) {
            return NORMAL;
        }

        for (TaskTrackerBehavior t : values()) {
            if (t.v.equals(type)) {
                return t;
            }
        }
        return NORMAL;
    }
}
