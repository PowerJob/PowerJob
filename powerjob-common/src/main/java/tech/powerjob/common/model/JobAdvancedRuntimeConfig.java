package tech.powerjob.common.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 任务运行时高级配置
 *
 * @author tjq
 * @since 2024/2/24
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class JobAdvancedRuntimeConfig {

    /**
     * MR 任务专享参数，TaskTracker 行为 {@link tech.powerjob.common.enums.TaskTrackerBehavior}
     */
    private Integer taskTrackerBehavior;

}
