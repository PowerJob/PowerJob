package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.ExecuteUtil;
import com.netease.mail.chronos.base.utils.TimeUtil;
import com.netease.mail.chronos.executor.support.common.TaskSplitParam;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.enums.RtTaskInstanceStatus;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import com.netease.mail.chronos.executor.support.service.auxiliary.impl.SpTaskInstanceHandleServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Date;
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

    /**
     * 每一批处理的数量
     */
    private static final Integer BATCH_SIZE = 100;
    /**
     * 最大处理数量
     */
    private static final Integer MAX_SIZE = 50_0000;
    /**
     * 间隙
     */
    private static final Long INTERVAL = 90_000L;

    private final Snowflake snowflake = IdUtil.getSnowflake();

    private final SpRemindTaskService spRemindTaskService;

    private final SpTaskInstanceHandleServiceImpl spTaskInstanceHandleService;


    /**
     * 上层传递触发时间
     * 根据触发时间获取 状态为有效，且下次触发时间小于当前时间 或 未来 90 s 内即将触发的任务
     * 处理后后计算下次触发时间，更新状态
     */
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        TaskSplitParam taskSplitParam = TaskSplitParam.parseOrDefault(context.getJobParams(), BATCH_SIZE, MAX_SIZE);
        long maxTriggerTime = System.currentTimeMillis() + INTERVAL;
        if (isRootTask()) {
            // （-,maxTriggerTime),limit
            List<Long> idList = spRemindTaskService.obtainValidTaskIdListByTriggerTimeThreshold(maxTriggerTime, taskSplitParam.getMaxSize());
            if (idList == null || idList.isEmpty()) {
                omsLogger.info("本次没有需要触发的提醒任务! maxTriggerTime:{} , limit:{}", maxTriggerTime, taskSplitParam.getMaxSize());
                return new ProcessResult(true, "本次没有需要触发的提醒任务");
            }
            // 小于阈值直接执行
            if (idList.size() <= taskSplitParam.getBatchSize()) {
                omsLogger.info("本次无需进行任务分片! 一共 {} 条", idList.size());
                processCore(0, idList, maxTriggerTime, omsLogger);
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
            processCore(subTask.getSeq(), idList, maxTriggerTime, omsLogger);
            omsLogger.info("处理任务分片({})成功,size:{}", subTask.getSeq(), subTask.getIdList().size());
            return new ProcessResult(true, "处理任务分片(" + subTask.getSeq() + ")成功!");
        }
    }

    private void processCore(int sliceSeq, List<Long> idList, long maxTriggerTime, OmsLogger omsLogger) {

        int errorCount = 0;
        long warnThreshold = System.currentTimeMillis() - INTERVAL;
        for (Long id : idList) {
            try {
                SpRemindTaskInfo spRemindTaskInfo = spRemindTaskService.selectById(id);
                // 判断是否需要跳过
                if (shouldSkip(maxTriggerTime, omsLogger, spRemindTaskInfo)) {
                    continue;
                }
                // INTERVAL 之前的任务 现在才触发，打印日志，表示这个任务延迟太严重，正常情况下不应该出现
                if (spRemindTaskInfo.getNextTriggerTime() < warnThreshold) {
                    log.warn("当前任务处理延迟过高(> {} ms),task detail:({})", INTERVAL, spRemindTaskInfo);
                }
                // 生成实例入库
                SpRtTaskInstance construct = construct(spRemindTaskInfo);
                // 这里会保证幂等性
                ExecuteUtil.executeIgnoreSpecifiedExceptionWithoutReturn(() -> spTaskInstanceHandleService.insert(construct), DuplicateKeyException.class);
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


    private SpRtTaskInstance construct(SpRemindTaskInfo spRemindTaskInfo) {

        SpRtTaskInstance spRtTaskInstance = new SpRtTaskInstance();
        // 基础信息
        spRtTaskInstance.setTaskId(spRemindTaskInfo.getId());
        spRtTaskInstance.setCustomId(spRemindTaskInfo.getCompId());
        spRtTaskInstance.setCustomKey(spRemindTaskInfo.getUid());
        spRtTaskInstance.setParam(spRemindTaskInfo.getParam());
        spRtTaskInstance.setExtra(spRemindTaskInfo.getExtra());
        // 运行信息
        spRtTaskInstance.setRunningTimes(0);
        // 最多重试 6 次
        spRtTaskInstance.setMaxRetryTimes(6);
        spRtTaskInstance.setExpectedTriggerTime(spRemindTaskInfo.getNextTriggerTime());
        spRtTaskInstance.setEnable(true);
        spRtTaskInstance.setStatus(RtTaskInstanceStatus.INIT.getCode());
        // 其他
        spRtTaskInstance.setCreateTime(new Date());
        spRtTaskInstance.setUpdateTime(new Date());
        spRtTaskInstance.setPartitionKey(TimeUtil.getDateNumber(new Date()));
        spRtTaskInstance.setId(snowflake.nextId());

        return spRtTaskInstance;

    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private String uid;
    }


    private boolean shouldSkip(long maxTriggerTime, OmsLogger omsLogger, SpRemindTaskInfo spRemindTaskInfo) {

        if (spRemindTaskInfo == null) {
            return true;
        }

        if (spRemindTaskInfo.getEnable() != null && !spRemindTaskInfo.getEnable()) {
            omsLogger.warn("提醒任务(id:{},colId:{},compId:{}) 已经被禁用，跳过处理", spRemindTaskInfo.getId(), spRemindTaskInfo.getColId(), spRemindTaskInfo.getCompId());
            return true;
        }
        // 检查 nextTriggerTime 是否已经变更（重试需要保证幂等）
        if (spRemindTaskInfo.getNextTriggerTime() == null
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
