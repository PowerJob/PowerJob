package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DispatchStrategy
 *
 * @author tjq
 * @since 2021/2/22
 */
@Getter
@AllArgsConstructor
public enum DispatchStrategy {

    HEALTH_FIRST(1),
    RANDOM(2);

    private final int v;

    public static DispatchStrategy of(Integer v) {
        if (v == null) {
            return HEALTH_FIRST;
        }
        for (DispatchStrategy ds : values()) {
            if (v.equals(ds.v)) {
                return ds;
            }
        }
        throw new IllegalArgumentException("unknown DispatchStrategy of " + v);
    }
}
