package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 日志级别
 *
 * @author tjq
 * @since 12/20/20
 */
@Getter
@AllArgsConstructor
public enum LogLevel {

    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    OFF(99);

    private final int v;

    public static String genLogLevelString(Integer v) {

        for (LogLevel logLevel : values()) {
            if (Objects.equals(logLevel.v, v)) {
                return logLevel.name();
            }
        }
        return "UNKNOWN";
    }
}
