package tech.powerjob.common.enums;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Scheduling time strategies
 *
 * @author tjq
 * @since 2020/3/30
 */
@Getter
@AllArgsConstructor
public enum TimeExpressionType {

    API(1),
    CRON(2),
    FIXED_RATE(3),
    FIXED_DELAY(4),
    WORKFLOW(5);

    int v;

    public static final List<Integer> frequentTypes = Lists.newArrayList(FIXED_RATE.v, FIXED_DELAY.v);

    public static TimeExpressionType of(int v) {
        for (TimeExpressionType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown TimeExpressionType of " + v);
    }
}
