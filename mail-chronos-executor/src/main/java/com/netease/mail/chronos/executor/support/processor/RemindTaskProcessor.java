package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import com.netease.mail.chronos.base.utils.ICalendarRecurrenceRuleUtil;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.common.po.TaskAdditionalData;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemindTaskProcessor implements MapProcessor {

    private static final Integer BATCH_SIZE = 50;
    /**
     * 间隙
     */
    private static final Long INTERVAL = 60_000L;

    private final SpRemindTaskService spRemindTaskService;


    /**
     * 上层传递触发时间
     * 根据触发时间获取 状态为有效，且下次触发时间小于当前时间 或 未来 30 s 内即将触发的任务
     * 派发后计算下次触发时间，更新状态
     */
    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger omsLogger = context.getOmsLogger();
        TaskAdditionalData additionalData = context.getAdditionalData();
        long minTriggerTime = additionalData.getOriginTriggerTime();
        long maxTriggerTime = minTriggerTime + INTERVAL;
        if (isRootTask()) {
            // 未来 60 s 以内 [minTriggerTime,maxTriggerTime)
            List<Long> idList = spRemindTaskService.obtainValidTaskIdListByTriggerTimeScope(minTriggerTime, maxTriggerTime);
            if (idList == null || idList.isEmpty()) {
                omsLogger.info("本次没有需要触发的提醒任务! minTriggerTime:{},maxTriggerTime:{}", minTriggerTime, maxTriggerTime);
                return new ProcessResult(true, "本次没有需要触发的提醒任务");
            }
            // 小于阈值直接执行
            if (idList.size() <= BATCH_SIZE) {
                omsLogger.info("本次无需进行任务分片! 一共 {} 条", minTriggerTime, maxTriggerTime);
                processCore(idList, minTriggerTime, maxTriggerTime, omsLogger);
                return new ProcessResult(true, "任务不需要分片,处理成功!");
            }
            List<SubTask> subTaskList = new LinkedList<>();
            // 切割任务
            List<List<Long>> idListList = CollUtil.split(idList, BATCH_SIZE);
            int count = 0;
            for (List<Long> list : idListList) {
                SubTask subTask = new SubTask(++count, list);
                subTaskList.add(subTask);
            }
            map(subTaskList, "ProcessRemindTask");
            return new ProcessResult(true, "切分任务成功");

        } else {
            SubTask subTask = (SubTask) context.getSubTask();
            omsLogger.info("开始处理任务分片 {},size:{}", subTask.getSeq(), subTask.getIdList().size());
            List<Long> idList = subTask.getIdList();
            processCore(idList, minTriggerTime, maxTriggerTime, omsLogger);
            omsLogger.info("处理任务分片({})成功,size:{}", subTask.getSeq(), subTask.getIdList().size());
            return new ProcessResult(true, "处理任务分片(" + subTask.getSeq() + ")成功!");
        }
    }

    private void processCore(List<Long> idList, long minTriggerTime, long maxTriggerTime, OmsLogger omsLogger) {
        for (Long id : idList) {
            SpRemindTaskInfo spRemindTaskInfo = spRemindTaskService.selectById(id);
            // 判断是否需要跳过
            if (shouldSkip(minTriggerTime, maxTriggerTime, omsLogger, spRemindTaskInfo)) {
                continue;
            }
            // 处理


            // 更新状态
            spRemindTaskInfo.setTriggerTimes(spRemindTaskInfo.getTriggerTimes() + 1);
            // 计算下次调度时间 , 理论上不应该会存在每分钟调度一次的提醒任务（业务场景决定）
            String recurrenceRule = spRemindTaskInfo.getRecurrenceRule();
            // 为空直接 disable (触发一次的任务)
            if (StringUtils.isBlank(recurrenceRule)) {
                disableTask(spRemindTaskInfo);
            } else {
                try {
                    // 更新 nextTriggerTime , 不处理 miss fire 的情形 ？
                    long nextTriggerTime = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime(spRemindTaskInfo.getRecurrenceRule(), spRemindTaskInfo.getStartTime(), System.currentTimeMillis());
                    // 检查生命周期
                    handleLifeCycle(spRemindTaskInfo, nextTriggerTime);
                } catch (Exception e) {
                    // 记录异常信息
                    omsLogger.error("处理任务(id:{},originId:{})失败，计算下次触发时间失败，已将其自动禁用，请检查重复规则表达式是否合法！recurrenceRule:{}", spRemindTaskInfo.getId(), spRemindTaskInfo.getOriginId(), spRemindTaskInfo.getRecurrenceRule(), e);
                    disableTask(spRemindTaskInfo);
                }
            }
            spRemindTaskInfo.setUpdateTime(new Date());
            spRemindTaskService.updateById(spRemindTaskInfo);
        }
    }

    private void handleLifeCycle(SpRemindTaskInfo spRemindTaskInfo, long nextTriggerTime) {
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


    private void disableTask(SpRemindTaskInfo spRemindTaskInfo) {
        spRemindTaskInfo.setEnable(false);
        spRemindTaskInfo.setDisableTime(new Date());
    }


    private boolean shouldSkip(long minTriggerTime, long maxTriggerTime, OmsLogger omsLogger, SpRemindTaskInfo spRemindTaskInfo) {
        if (spRemindTaskInfo.getEnable() != null && !spRemindTaskInfo.getEnable()) {
            omsLogger.warn("提醒任务 (id:{},originId:{}) 已经被禁用，跳过处理", spRemindTaskInfo.getId(), spRemindTaskInfo.getOriginId());
            return true;
        }
        // 检查 nextTriggerTime 是否已经变更（重试需要保证幂等）
        if (spRemindTaskInfo.getNextTriggerTime() == null
                || spRemindTaskInfo.getNextTriggerTime() < minTriggerTime
                || spRemindTaskInfo.getNextTriggerTime() >= maxTriggerTime) {
            omsLogger.warn("提醒任务 (id:{},originId:{}) 本次调度已被成功处理过，跳过", spRemindTaskInfo.getId(), spRemindTaskInfo.getOriginId());
            return true;
        }
        return false;
    }


    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    public static class SubTask {

        private int seq;

        private List<Long> idList;

    }

}
