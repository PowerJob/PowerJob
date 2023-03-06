package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LogType
 *
 * @author tjq
 * @since 2022/10/3
 */
@Getter
@AllArgsConstructor
public enum LogType {
    ONLINE(1),
    LOCAL(2),
    STDOUT(3),

    LOCAL_AND_ONLINE(4),

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
