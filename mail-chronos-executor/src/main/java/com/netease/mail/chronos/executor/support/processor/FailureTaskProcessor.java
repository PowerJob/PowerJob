package com.netease.mail.chronos.executor.support.processor;

import cn.hutool.core.collection.CollUtil;
import com.netease.mail.chronos.executor.support.common.CommonLogic;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Date;
import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailureTaskProcessor  implements BasicProcessor {

    private final SpRemindTaskService spRemindTaskService;


    @Override
    public ProcessResult process(TaskContext context){
        OmsLogger omsLogger = context.getOmsLogger();
        // 5 分钟内没有触发，且没有被 disable
        List<SpRemindTaskInfo> spRemindTaskInfos = spRemindTaskService.obtainStagnantTask();
        // 尝试更新 next_trigger_time
        if (CollUtil.isEmpty(spRemindTaskInfos)) {
            omsLogger.info("暂无需要处理的停滞任务！");
            return new ProcessResult(true,"暂无需要处理的停滞任务！");
        }
        omsLogger.info("开始处理停滞任务！共计 {} 条！",spRemindTaskInfos.size());
        for (SpRemindTaskInfo spRemindTaskInfo : spRemindTaskInfos) {
            CommonLogic.updateTriggerTime(omsLogger,spRemindTaskInfo);
            spRemindTaskService.updateById(spRemindTaskInfo);
            omsLogger.info("处理任务:{}",spRemindTaskInfo);
        }
        omsLogger.info("处理停滞任务完成！共计 {} 条！",spRemindTaskInfos.size());
        return new ProcessResult(true,"处理停滞任务完成！count:"+spRemindTaskInfos.size());
    }

}
