package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.ExceptionUtil;
import com.netease.mail.chronos.base.utils.ExecuteUtil;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.common.TaskSplitParam;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.enums.RtTaskInstanceStatus;
import com.netease.mail.chronos.executor.support.service.NotifyService;
import com.netease.mail.chronos.executor.support.service.auxiliary.impl.SpTaskInstanceHandleServiceImpl;
import com.netease.mail.uaInfo.UaInfoContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.netease.mail.chronos.executor.support.common.CommonLogic.*;

/**
 * @author Echo009
 * @since 2021/10/29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RtTaskInstanceProcessor implements MapProcessor {

    private static final HashMap<String, Object> FAKE_UA = Maps.newHashMap();
    /**
     * 每一批处理的数量
     */
    private static final Integer BATCH_SIZE = 100;
    /**
     * 最大处理数量
     */
    private static final Integer MAX_SIZE = 50_0000;

    static {
        FAKE_UA.put("fakeUa", "ignore");
    }


    private final NotifyService notifyService;


    private final SpTaskInstanceHandleServiceImpl spTaskInstanceHandleService;


    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        TaskSplitParam taskSplitParam = TaskSplitParam.parseOrDefault(context.getJobParams(),BATCH_SIZE,MAX_SIZE);
        if (isRootTask()) {
            List<TaskInstancePrimaryKey> taskInstancePrimaryKeys = spTaskInstanceHandleService.loadHandleInstanceIdList(taskSplitParam.getMaxSize());
            if (taskInstancePrimaryKeys == null || taskInstancePrimaryKeys.isEmpty()) {
                omsLogger.info("本次没有需要处理的提醒任务实例! ");
                return new ProcessResult(true, "本次没有需要处理的提醒任务实例");
            }
            // 小于阈值直接执行
            if (taskInstancePrimaryKeys.size() <= taskSplitParam.getBatchSize()) {
                omsLogger.info("本次无需进行任务分片! 一共 {} 条", taskInstancePrimaryKeys.size());
                processCore(0, taskInstancePrimaryKeys, omsLogger);
                return new ProcessResult(true, "任务不需要分片,处理成功!");
            }
            omsLogger.info("开始切分任务! batchSize:{}", taskSplitParam.getBatchSize());
            List<RtTaskInstanceProcessor.SubTask> subTaskList = new LinkedList<>();
            // 切割任务
            List<List<TaskInstancePrimaryKey>> idListList = CollUtil.split(taskInstancePrimaryKeys, taskSplitParam.getBatchSize());
            int count = 0;
            for (List<TaskInstancePrimaryKey> list : idListList) {
                // start from 1
                RtTaskInstanceProcessor.SubTask subTask = new RtTaskInstanceProcessor.SubTask(++count, list);
                subTaskList.add(subTask);
            }
            map(subTaskList, "ProcessRemindTaskInstance");
            return new ProcessResult(true, "切分任务成功,total:" + subTaskList.size());

        } else {
            RtTaskInstanceProcessor.SubTask subTask = (RtTaskInstanceProcessor.SubTask) context.getSubTask();
            omsLogger.info("开始处理任务分片 {},size:{}", subTask.getSeq(), subTask.getIdList().size());
            List<TaskInstancePrimaryKey> idList = subTask.getIdList();
            processCore(subTask.getSeq(), idList, omsLogger);
            omsLogger.info("处理任务分片({})成功,size:{}", subTask.getSeq(), subTask.getIdList().size());
            return new ProcessResult(true, "处理任务分片(" + subTask.getSeq() + ")成功!");
        }
    }

    @SuppressWarnings("squid:S3776")
    private void processCore(int sliceSeq, List<TaskInstancePrimaryKey> idList, OmsLogger omsLogger) {
        // 新版本的 feign 会去掉 {} 故不能传空对象
        UaInfoContext.setUaInfo(FAKE_UA);
        int errorCount = 0;
        for (TaskInstancePrimaryKey id : idList) {
            SpRtTaskInstance spRtTaskInstance = null;
            try {
                spRtTaskInstance = spTaskInstanceHandleService.selectByPrimaryKey(id);
                // 判断是否需要跳过
                if (shouldSkip(omsLogger, spRtTaskInstance)) {
                    // 更新状态为禁用
                    if (spRtTaskInstance != null) {
                        disableInstance(spRtTaskInstance);
                        spTaskInstanceHandleService.updateByPrimaryKey(spRtTaskInstance);
                    }
                    continue;
                }
                // 记录首次触发时间
                if (spRtTaskInstance.getActualTriggerTime() == null) {
                    spRtTaskInstance.setActualTriggerTime(System.currentTimeMillis());
                }
                // 处理
                boolean res = notifyService.sendNotify(spRtTaskInstance, omsLogger);
                // 记录完成时间
                spRtTaskInstance.setFinishedTime(System.currentTimeMillis());
                // 更新状态
                spRtTaskInstance.setStatus(res ? RtTaskInstanceStatus.SUCCESS.getCode() : RtTaskInstanceStatus.FAILED.getCode());
            } catch (Exception e) {
                // 数据库异常 或者 网络异常
                omsLogger.error("处理任务(id:{})失败 ！", id, e);
                errorCount++;
                if (spRtTaskInstance != null) {
                    spRtTaskInstance.setResult(ExceptionUtil.getExceptionDesc(e));
                    spRtTaskInstance.setStatus(RtTaskInstanceStatus.FAILED.getCode());
                }
            }
            if (spRtTaskInstance != null) {
                // 更新运行次数
                spRtTaskInstance.setRunningTimes(spRtTaskInstance.getRunningTimes() + 1);
                // update
                final SpRtTaskInstance f = spRtTaskInstance;
                // 如果这里失败了，本轮不重试，等下一轮调度
                ExecuteUtil.executeIgnoreExceptionWithoutReturn(() -> spTaskInstanceHandleService.updateByPrimaryKey(f), "update remind task instance:" + f.getId());
            }
        }
        if (errorCount != 0) {
            omsLogger.info("处理任务分片({})失败,total:{},failure:{}", sliceSeq, idList.size(), errorCount);
            throw new BaseException("任务分片处理失败,seq:" + sliceSeq);
        }
    }


    private boolean shouldSkip(OmsLogger omsLogger, SpRtTaskInstance spRtTaskInstance) {
        if (spRtTaskInstance == null) {
            return true;
        }
        if (spRtTaskInstance.getStatus() != null && spRtTaskInstance.getStatus().equals(RtTaskInstanceStatus.SUCCESS.getCode())) {
            omsLogger.warn("提醒任务实例(id:{},compId:{},expectedTriggerTime:{}) 已执行成功，跳过处理", spRtTaskInstance.getId(), spRtTaskInstance.getCustomId(), spRtTaskInstance.getExpectedTriggerTime());
            return true;
        }
        if (spRtTaskInstance.getEnable() != null && !spRtTaskInstance.getEnable()) {
            omsLogger.warn("提醒任务实例(id:{},compId:{},expectedTriggerTime:{}) 已经被禁用，跳过处理", spRtTaskInstance.getId(), spRtTaskInstance.getCustomId(), spRtTaskInstance.getExpectedTriggerTime());
            return true;
        }
        // 检查是否已经超过最大重试次数
        if (spRtTaskInstance.getRunningTimes() != null
                && spRtTaskInstance.getMaxRetryTimes() != null
                && spRtTaskInstance.getRunningTimes() > spRtTaskInstance.getMaxRetryTimes()) {
            omsLogger.warn("提醒任务实例(id:{},compId:{},expectedTriggerTime:{})已超过最大运行次数", spRtTaskInstance.getId(), spRtTaskInstance.getCustomId(), spRtTaskInstance.getExpectedTriggerTime());
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

        private List<TaskInstancePrimaryKey> idList;

    }




}
