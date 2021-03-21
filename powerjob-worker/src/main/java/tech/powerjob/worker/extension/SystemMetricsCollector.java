package tech.powerjob.worker.extension;

import tech.powerjob.common.model.SystemMetrics;

/**
 * user-customized system metrics collector
 *
 * @author tjq
 * @since 2021/2/16
 */
public interface SystemMetricsCollector {

    /**
     * SystemMetrics, you can put your custom metrics info in the 'extra' param
     * @return SystemMetrics
     */
    SystemMetrics collect();
}
