package tech.powerjob.samples.processors;

import org.apache.commons.lang3.StringUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 单机处理器 示例
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
@Component("testBaseProcessor")
public class StandaloneProcessorDemo implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("StandaloneProcessorDemo start process,context is {}.", context);
        omsLogger.info("Notice! If you want this job process failed, your jobParams need to be 'failed'");
        omsLogger.info("Let's test the exception~");
        // 测试异常日志
        try {
            Collections.emptyList().add("277");
        } catch (Exception e) {
            omsLogger.error("oh~it seems that we have an exception~", e);
        }
        log.info("================ StandaloneProcessorDemo#process ================");
        log.info("jobParam:{}", context.getJobParams());
        log.info("instanceParams:{}", context.getInstanceParams());
        String param;
        // 解析参数，非处于工作流中时，优先取实例参数（允许动态[instanceParams]覆盖静态参数[jobParams]）
        if (context.getWorkflowContext() == null) {
            param = StringUtils.isBlank(context.getInstanceParams()) ? context.getJobParams() : context.getInstanceParams();
        } else {
            param = context.getJobParams();
        }
        // 根据参数判断是否成功
        boolean success = !"failed".equals(param);
        omsLogger.info("StandaloneProcessorDemo finished process,success: {}", success);
        omsLogger.info("anyway, we finished the job successfully~Congratulations!");
        return new ProcessResult(success, context + ": " + success);
    }
}
