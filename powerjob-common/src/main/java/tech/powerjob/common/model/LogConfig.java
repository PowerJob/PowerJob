package tech.powerjob.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 任务日志配置
 *
 * @author yhz
 * @since 2022/9/16
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class LogConfig {
    /**
     * log type {@link tech.powerjob.common.enums.LogType}
     */
    private Integer type;
    /**
     * log level {@link tech.powerjob.common.enums.LogLevel}
     */
    private Integer level;

    private String loggerName;


}
