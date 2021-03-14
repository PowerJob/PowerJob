package tech.powerjob.samples.processors;

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
@Component
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
        }catch (Exception e) {
            omsLogger.error("oh~it seems that we have an exception~", e);
        }

        System.out.println("================ StandaloneProcessorDemo#process ================");
        System.out.println(context.getJobParams());
        // 根据控制台参数判断是否成功
        boolean success = !"failed".equals(context.getJobParams());
        omsLogger.info("StandaloneProcessorDemo finished process,success: .", success);

        omsLogger.info("anyway, we finished the job successfully~Congratulations!");
        return new ProcessResult(success, context + ": " + success);
    }
}
