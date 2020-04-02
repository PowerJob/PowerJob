package com.github.kfcfans.oms.server.common.constans;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 时间表达式类型
 *
 * @author tjq
 * @since 2020/3/30
 */
@Getter
@AllArgsConstructor
public enum TimeExpressionType {

    API(1),
    CRON(2),
    FIX_RATE(3),
    FIX_DELAY(4);

    int v;
}
