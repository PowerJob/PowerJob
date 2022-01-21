package tech.powerjob.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmConfig {
    /**
     * 触发告警的阈值
     */
    private Integer alertThreshold;
    /**
     * 统计的窗口长度（s）
     */
    private Integer statisticWindowLen;
    /**
     * 沉默时间窗口（s）
     */
    private Integer silenceWindowLen;

}
