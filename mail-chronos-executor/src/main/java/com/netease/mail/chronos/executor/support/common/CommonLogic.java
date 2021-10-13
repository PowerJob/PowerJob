package com.netease.mail.chronos.executor.support.common;

import com.netease.mail.chronos.base.utils.ICalendarRecurrenceRuleUtil;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Date;

/**
 * @author Echo009
 * @since 2021/9/30
 */
public class CommonLogic {


    public static void updateTriggerTime(OmsLogger omsLogger, SpRemindTaskInfo spRemindTaskInfo) {
        try {
            // 更新 nextTriggerTime , 不处理 miss fire 的情形 ？
            long nextTriggerTime = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime(spRemindTaskInfo.getRecurrenceRule(), spRemindTaskInfo.getStartTime() + spRemindTaskInfo.getTriggerOffset(), System.currentTimeMillis());
            // 检查生命周期
            handleLifeCycle(spRemindTaskInfo, nextTriggerTime);
        } catch (Exception e) {
            // 记录异常信息
            omsLogger.error("处理任务(id:{},colId:{},compId:{})失败，计算下次触发时间失败，已将其自动禁用，请检查重复规则表达式是否合法！recurrenceRule:{}", spRemindTaskInfo.getId(), spRemindTaskInfo.getColId(),spRemindTaskInfo.getCompId(), spRemindTaskInfo.getRecurrenceRule(), e);
            disableTask(spRemindTaskInfo);
        }
    }

    private static void handleLifeCycle(SpRemindTaskInfo spRemindTaskInfo, long nextTriggerTime) {
        // 当不存在下一次调度时间时，nextTriggerTime = 0
        if (nextTriggerTime == 0L) {
            disableTask(spRemindTaskInfo);
        } else if (spRemindTaskInfo.getEndTime() != null && spRemindTaskInfo.getEndTime() < nextTriggerTime) {
            disableTask(spRemindTaskInfo);
        } else if (spRemindTaskInfo.getTimesLimit() > 0 && spRemindTaskInfo.getTriggerTimes() >= spRemindTaskInfo.getTimesLimit()) {
            disableTask(spRemindTaskInfo);
        } else {
            spRemindTaskInfo.setNextTriggerTime(nextTriggerTime);
        }
    }


    public static void disableTask(SpRemindTaskInfo spRemindTaskInfo) {
        spRemindTaskInfo.setEnable(false);
        spRemindTaskInfo.setDisableTime(new Date());
    }


}
