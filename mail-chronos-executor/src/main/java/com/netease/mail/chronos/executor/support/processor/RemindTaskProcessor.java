package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.service.NotifyService;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import com.netease.mail.uaInfo.UaInfoContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.netease.mail.chronos.executor.support.common.CommonLogic.disableTask;
import static com.netease.mail.chronos.executor.support.common.CommonLogic.updateTriggerTime;

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

    private final NotifyService notifyService;

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
                omsLogger.info("本次无需进行任务分片! 一共 {} 条", idList.size());
                processCore(0, idList, minTriggerTime, maxTriggerTime, omsLogger);
                return new ProcessResult(true, "任务不需要分片,处理成功!");
            }
            omsLogger.info("开始切分任务! batchSize:{}", BATCH_SIZE);
            List<SubTask> subTaskList = new LinkedList<>();
            // 切割任务
            List<List<Long>> idListList = CollUtil.split(idList, BATCH_SIZE);
            int count = 0;
            for (List<Long> list : idListList) {
                // start from 1
                SubTask subTask = new SubTask(++count, list);
                subTaskList.add(subTask);
            }
            map(subTaskList, "ProcessRemindTask");
            return new ProcessResult(true, "切分任务成功,total:" + subTaskList.size());

        } else {
            SubTask subTask = (SubTask) context.getSubTask();
            omsLogger.info("开始处理任务分片 {},size:{}", subTask.getSeq(), subTask.getIdList().size());
            List<Long> idList = subTask.getIdList();
            processCore(subTask.getSeq(), idList, minTriggerTime, maxTriggerTime, omsLogger);
            omsLogger.info("处理任务分片({})成功,size:{}", subTask.getSeq(), subTask.getIdList().size());
            return new ProcessResult(true, "处理任务分片(" + subTask.getSeq() + ")成功!");
        }
    }

    private void processCore(int sliceSeq, List<Long> idList, long minTriggerTime, long maxTriggerTime, OmsLogger omsLogger) {

        // 新版本的 feign 会去掉 {} 故不能传空对象
        HashMap<String, Object> fakeUa = Maps.newHashMap();
        fakeUa.put("fakeUa", "ignore");
        UaInfoContext.setUaInfo(fakeUa);
        int errorCount = 0;
        for (Long id : idList) {
            try {
                SpRemindTaskInfo spRemindTaskInfo = spRemindTaskService.selectById(id);
                // 判断是否需要跳过
                if (shouldSkip(minTriggerTime, maxTriggerTime, omsLogger, spRemindTaskInfo)) {
                    continue;
                }
                // 处理
                notifyService.sendNotify(spRemindTaskInfo, omsLogger);
                // 更新状态
                spRemindTaskInfo.setTriggerTimes(spRemindTaskInfo.getTriggerTimes() + 1);
                // 计算下次调度时间 , 理论上不应该会存在每分钟调度一次的提醒任务（业务场景决定）
                String recurrenceRule = spRemindTaskInfo.getRecurrenceRule();
                // 为空直接 disable (触发一次的任务)
                if (StringUtils.isBlank(recurrenceRule)) {
                    disableTask(spRemindTaskInfo);
                } else {
                    updateTriggerTime(omsLogger, spRemindTaskInfo);
                }
                spRemindTaskInfo.setUpdateTime(new Date());
                spRemindTaskService.updateById(spRemindTaskInfo);
            } catch (Exception e) {
                omsLogger.error("处理任务(id:{})失败 ！", id, e);
                errorCount++;
            }
        }
        if (errorCount != 0) {
            omsLogger.info("处理任务分片({})失败,total:{},failure:{}", sliceSeq, idList.size(), errorCount);
            throw new BaseException("任务分片处理失败,seq:" + sliceSeq);
        }
    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private String uid;
    }


    private boolean shouldSkip(long minTriggerTime, long maxTriggerTime, OmsLogger omsLogger, SpRemindTaskInfo spRemindTaskInfo) {
        if (spRemindTaskInfo.getEnable() != null && !spRemindTaskInfo.getEnable()) {
            omsLogger.warn("提醒任务(id:{},colId:{},compId:{}) 已经被禁用，跳过处理", spRemindTaskInfo.getId(), spRemindTaskInfo.getColId(), spRemindTaskInfo.getCompId());
            return true;
        }
        // 检查 nextTriggerTime 是否已经变更（重试需要保证幂等）
        if (spRemindTaskInfo.getNextTriggerTime() == null
                || spRemindTaskInfo.getNextTriggerTime() < minTriggerTime
                || spRemindTaskInfo.getNextTriggerTime() >= maxTriggerTime) {
            omsLogger.warn("提醒任务(id:{},colId:{},compId:{})本次调度已被成功处理过，跳过", spRemindTaskInfo.getId(), spRemindTaskInfo.getColId(), spRemindTaskInfo.getCompId());
            return true;
        }
        return false;
    }


    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubTask {

        private int seq;

        private List<Long> idList;

    }

}
