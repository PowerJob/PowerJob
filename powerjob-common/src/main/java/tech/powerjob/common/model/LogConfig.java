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
     * log type {@link LogType}
     */
    private Integer type;
    /**
     * log level {@link tech.powerjob.common.enums.LogLevel}
     */
    private Integer level;

    private String loggerName;

    @Getter
    @AllArgsConstructor
    public enum LogType {
        ONLINE(1),
        LOCAL(2),
        STDOUT(3),

        NULL(999);
        private final Integer v;

        public static LogType of(Integer type) {

            if (type == null) {
                return ONLINE;
            }

            for (LogType logType : values()) {
                if (logType.v.equals(type)) {
                    return logType;
                }
            }
            return ONLINE;
        }
    }
}
