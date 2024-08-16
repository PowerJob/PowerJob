package tech.powerjob.worker.sdk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tech.powerjob.common.enums.TimeExpressionType;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/17
 */
@Getter
@RequiredArgsConstructor
public class TimeExpression {

    private final TimeExpressionType timeExpressionType;

    private final String timeExpression;

    public static TimeExpression of(TimeExpressionType timeExpressionType, String timeExpression) {
        return new TimeExpression(timeExpressionType, timeExpression);
    }
}
