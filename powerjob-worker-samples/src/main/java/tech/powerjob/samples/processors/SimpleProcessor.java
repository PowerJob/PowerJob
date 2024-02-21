package tech.powerjob.samples.processors;

import tech.powerjob.official.processors.impl.ConfigProcessor;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Map;
import java.util.Optional;

/**
 * @author Echo009
 * @since 2022/4/27
 */
public class SimpleProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger logger = context.getOmsLogger();

        String jobParams = Optional.ofNullable(context.getJobParams()).orElse("S");
        logger.info("Current context:{}", context.getWorkflowContext());
        logger.info("Current job params:{}", jobParams);

        // 测试中文问题 #581
        if (jobParams.contains("CN")) {
            return new ProcessResult(true, "任务成功啦！！！");
        }

        // 测试配置中心获取数据
        Map<String, Object> dynamicConfig = ConfigProcessor.fetchConfig();
        Object valueA = dynamicConfig.get("keyA");
        logger.info("[Test] dynamicConfig: {}, fetchByKeyA: {}", dynamicConfig, valueA);

        return jobParams.contains("F") ? new ProcessResult(false) : new ProcessResult(true, "yeah!");

    }
}