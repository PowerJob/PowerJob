package tech.powerjob.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task group quota configuration for per-group thread pool isolation.
 * Each task group gets its own isolated lightweight thread pool on the worker when configured.
 *
 * @since 2026/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskGroupQuota {

    /**
     * Default task group name. Jobs without an explicit taskGroup are assigned to this group.
     */
    public static final String DEFAULT_GROUP = "default";

    /**
     * Normalize a raw task group string: null/empty -> DEFAULT_GROUP, otherwise trimmed.
     */
    public static String normalizeTaskGroup(String raw) {
        return (raw == null || raw.trim().isEmpty()) ? DEFAULT_GROUP : raw.trim();
    }

    /**
     * Task group name (e.g., "payments", "reports", "default").
     * Informational only when used in Spring Boot properties — the map key determines the group name.
     * Required when constructing programmatically.
     */
    private String groupName;

    /**
     * Maximum number of lightweight task trackers for this group.
     * Only applies to standalone CRON/API/DailyInterval jobs (lightweight tasks).
     * Heavyweight tasks (FixedRate/FixedDelay/Broadcast/Map/MapReduce) are not affected.
     */
    private int maxLightweightTaskNum;
}
