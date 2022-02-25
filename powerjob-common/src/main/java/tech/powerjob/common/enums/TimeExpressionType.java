package tech.powerjob.common.enums;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Scheduling time strategies
 *
 * @author tjq
 * @since 2020/3/30
 */
@Getter
@AllArgsConstructor
@ToString
public enum TimeExpressionType {

    API(1),
    CRON(2),
    FIXED_RATE(3),
    FIXED_DELAY(4),
    WORKFLOW(5);

    int v;

    public static final List<Integer> FREQUENT_TYPES = Lists.newArrayList(FIXED_RATE.v, FIXED_DELAY.v);
    /**
     * 首次计算触发时间时必须计算出一个有效值
     */
    public static final List<Integer> INSPECT_TYPES =  Lists.newArrayList(CRON.v);

    public static TimeExpressionType of(int v) {
        for (TimeExpressionType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown TimeExpressionType of " + v);
    }
}
