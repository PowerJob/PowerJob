package tech.powerjob.server.core.service;

import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.server.common.utils.CronExpression;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 校验服务
 *
 * @author tjq
 * @since 2020/11/28
 */
public class ValidateService {

    private static final int NEXT_N_TIMES = 5;

    /**
     * 计算指定时间表达式接下来的运行状况
     * @param timeExpressionType 时间表达式类型
     * @param timeExpression 时间表达式
     * @return 最近 N 次运行的时间
     * @throws Exception 异常
     */
    public static List<String> calculateNextTriggerTime(TimeExpressionType timeExpressionType, String timeExpression) throws Exception {
        switch (timeExpressionType) {
            case API: return Lists.newArrayList(OmsConstant.NONE);
            case WORKFLOW: return Lists.newArrayList("VALID: depends on workflow");
            case CRON: return calculateCronExpression(timeExpression);
            case FIXED_RATE: return calculateFixRate(timeExpression);
            case FIXED_DELAY: return Lists.newArrayList("VALID: depends on execution cost time");
        }
        // impossible
        return Collections.emptyList();
    }


    private static List<String> calculateFixRate(String timeExpression) {
        List<String> result = Lists.newArrayList();
        long delay = Long.parseLong(timeExpression);
        for (int i = 0; i < NEXT_N_TIMES; i++) {
            long nextTime = System.currentTimeMillis() + i * delay;
            result.add(DateFormatUtils.format(nextTime, OmsConstant.TIME_PATTERN));
        }
        return result;
    }

    private static List<String> calculateCronExpression(String expression) throws ParseException {
        CronExpression cronExpression = new CronExpression(expression);
        List<String> result = Lists.newArrayList();
        Date time = new Date();
        for (int i = 0; i < NEXT_N_TIMES; i++) {
            Date nextValidTime = cronExpression.getNextValidTimeAfter(time);
            if (nextValidTime == null) {
                break;
            }
            result.add(DateFormatUtils.format(nextValidTime.getTime(), OmsConstant.TIME_PATTERN));
            time = nextValidTime;
        }
        if (result.isEmpty()) {
            result.add("INVALID: no next validate schedule time");
        }
        return result;
    }
}
