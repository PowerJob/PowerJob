package tech.powerjob.server.core.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.scheduler.auxiliary.TimingStrategyHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Echo009
 * @since 2022/3/21
 */
@Slf4j
@Service
public class TimingStrategyService {

    private static final int NEXT_N_TIMES = 5;

    private static final List<String> TIPS = Collections.singletonList("It is valid, but has not trigger time list!");


    private final Map<TimeExpressionType, TimingStrategyHandler> strategyContainer;

    public TimingStrategyService(List<TimingStrategyHandler> timingStrategyHandlers) {
        // init
        strategyContainer = new EnumMap<>(TimeExpressionType.class);
        for (TimingStrategyHandler timingStrategyHandler : timingStrategyHandlers) {
            strategyContainer.put(timingStrategyHandler.supportType(), timingStrategyHandler);
        }
    }

    /**
     * 计算接下来几次的调度时间
     *
     * @param timeExpressionType 定时表达式类型
     * @param timeExpression     表达式
     * @param startTime          起始时间(include)
     * @param endTime            结束时间(include)
     * @return 调度时间列表
     */
    public List<String> calculateNextTriggerTimes(TimeExpressionType timeExpressionType, String timeExpression, Long startTime, Long endTime) {

        TimingStrategyHandler timingStrategyHandler = getHandler(timeExpressionType);
        List<Long> triggerTimeList = new ArrayList<>(NEXT_N_TIMES);
        Long nextTriggerTime = System.currentTimeMillis();
        do {
            nextTriggerTime = timingStrategyHandler.calculateNextTriggerTime(nextTriggerTime, timeExpression, startTime, endTime);
            if (nextTriggerTime == null) {
                break;
            }
            triggerTimeList.add(nextTriggerTime);
        } while (triggerTimeList.size() < NEXT_N_TIMES);

        if (triggerTimeList.isEmpty()) {
            return TIPS;
        }
        return triggerTimeList.stream().map(t -> DateFormatUtils.format(t, OmsConstant.TIME_PATTERN)).collect(Collectors.toList());
    }

    /**
     * 计算下次的调度时间
     *
     * @param preTriggerTime     上次触发时间(nullable)
     * @param timeExpressionType 定时表达式类型
     * @param timeExpression     表达式
     * @param startTime          起始时间(include)
     * @param endTime            结束时间(include)
     * @return 下次的调度时间
     */
    public Long calculateNextTriggerTime(Long preTriggerTime, TimeExpressionType timeExpressionType, String timeExpression, Long startTime, Long endTime) {
        if (preTriggerTime == null || preTriggerTime < System.currentTimeMillis()) {
            preTriggerTime = System.currentTimeMillis();
        }
        return getHandler(timeExpressionType).calculateNextTriggerTime(preTriggerTime, timeExpression, startTime, endTime);
    }


    /**
     * 计算下次的调度时间并检查校验规则
     *
     * @param timeExpressionType 定时表达式类型
     * @param timeExpression     表达式
     * @param startTime          起始时间(include)
     * @param endTime            结束时间(include)
     * @return 下次的调度时间
     */
    public Long calculateNextTriggerTimeWithInspection( TimeExpressionType timeExpressionType, String timeExpression, Long startTime, Long endTime) {
        Long nextTriggerTime = calculateNextTriggerTime(null, timeExpressionType, timeExpression, startTime, endTime);
        if (TimeExpressionType.INSPECT_TYPES.contains(timeExpressionType.getV()) && nextTriggerTime == null) {
            throw new PowerJobException("time expression is out of date: " + timeExpression);
        }
        return nextTriggerTime;
    }


    public void validate(TimeExpressionType timeExpressionType, String timeExpression, Long startTime, Long endTime) {
        if (endTime != null) {
            if (endTime <= System.currentTimeMillis()) {
                throw new PowerJobException("lifecycle is out of date!");
            }
            if (startTime != null && startTime > endTime) {
                throw new PowerJobException("lifecycle is invalid! start time must earlier then end time.");
            }
        }
        getHandler(timeExpressionType).validate(timeExpression);
    }


    private TimingStrategyHandler getHandler(TimeExpressionType timeExpressionType) {
        TimingStrategyHandler timingStrategyHandler = strategyContainer.get(timeExpressionType);
        if (timingStrategyHandler == null) {
            throw new PowerJobException("No matching TimingStrategyHandler for this TimeExpressionType:" + timeExpressionType);
        }
        return timingStrategyHandler;
    }

}
