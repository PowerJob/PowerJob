package tech.powerjob.samples.processors.test;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <a href="https://github.com/PowerJob/PowerJob/issues/1033">测试长时间执行的任务 idle 导致 reduce 不执行</a>
 *
 * @author tjq
 * @since 2024/11/21
 */
@Slf4j
@Component
public class IdleBugTestProcessor implements MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        if (isRootTask()) {
            map(Lists.newArrayList("1", "2", "3", "4", "5", "6", "7"), "L1_TASK");
            return new ProcessResult(true, "MAP_SUCCESS");
        }

        Object subTask = context.getSubTask();
        log.info("[IdleBugTestProcessor] subTask:={}, start to process!", subTask);

        // 同步修改 idle 阈值
        CommonUtils.easySleep(ThreadLocalRandom.current().nextInt(40001, 60000));
        log.info("[IdleBugTestProcessor] subTask:={}, finished process", subTask);
        return new ProcessResult(true, "SUCCESS_" + subTask);
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        log.info("[IdleBugTestProcessor] [REDUCE] REDUCE!!!");
        return new ProcessResult(true, "SUCCESS");
    }
}
