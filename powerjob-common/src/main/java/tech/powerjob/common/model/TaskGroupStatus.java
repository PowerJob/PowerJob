package tech.powerjob.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Runtime status of a task group on a worker.
 * Reported in worker heartbeat for server-side group-aware dispatch.
 *
 * @since 2026/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroupStatus {

    /**
     * Task group name
     */
    private String groupName;

    /**
     * Current number of running lightweight task trackers in this group
     */
    private int currentLightweightTaskNum;

    /**
     * Maximum lightweight task tracker capacity for this group (from TaskGroupQuota config)
     */
    private int maxLightweightTaskNum;

    /**
     * Whether this group is at or over capacity (currentLightweightTaskNum >= maxLightweightTaskNum)
     */
    private boolean overloaded;
}
