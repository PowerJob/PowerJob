package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutOfDateTaskProcessor implements BasicProcessor {

    private final SpRemindTaskService spRemindTaskService;


    @Override
    public ProcessResult process(TaskContext context) {

        OmsLogger omsLogger = context.getOmsLogger();

        List<SpRemindTaskInfo> outOfDateDisableTaskList = spRemindTaskService.obtainOutOfDateDisableTask();

        if (CollUtil.isEmpty(outOfDateDisableTaskList)) {
            omsLogger.info("暂无需要清理的过期任务！");
            return new ProcessResult(true,"没有需要清理的过期任务！");
        }
        omsLogger.info("开始清理过期任务！共计 {} 条！",outOfDateDisableTaskList.size());
        for (SpRemindTaskInfo spRemindTaskInfo : outOfDateDisableTaskList) {
            spRemindTaskService.deleteById(spRemindTaskInfo.getId());
            omsLogger.info("清理任务 {} 完成!",spRemindTaskInfo);
        }
        omsLogger.info("清理过期任务完成！共计 {} 条！",outOfDateDisableTaskList.size());
        return new ProcessResult(true,"清理过期任务完成！count:"+outOfDateDisableTaskList.size());
    }
}
