package tech.powerjob.samples.mr;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.log.OmsLogger;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MapReduce 模拟 静态分片
 * 典型的杀鸡焉用牛刀～
 *
 * @author tjq
 * @since 2020/5/21
 */
@Component
public class StaticSliceProcessor implements MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();

        // root task 负责分发任务
        if (isRootTask()) {
            // 从控制台传递分片参数，假设格式为KV：1=a&2=b&3=c
            String jobParams = context.getJobParams();
            Map<String, String> paramsMap = Splitter.on("&").withKeyValueSeparator("=").split(jobParams);

            List<SubTask> subTasks = Lists.newLinkedList();
            paramsMap.forEach((k, v) -> subTasks.add(new SubTask(Integer.parseInt(k), v)));
            map(subTasks, "SLICE_TASK");
            return new ProcessResult(true, "map successfully");
        }

        Object subTask = context.getSubTask();
        if (subTask instanceof SubTask) {
            // 实际处理
            // 当然，如果觉得 subTask 还是很大，也可以继续分发哦

            return new ProcessResult(true, "subTask:" + ((SubTask) subTask).getIndex() + " process successfully");
        }
        return new ProcessResult(false, "UNKNOWN BUG");
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        // 按需求做一些统计工作... 不需要的话，直接使用 Map 处理器即可
        return new ProcessResult(true, "xxxx");
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SubTask {
        private int index;
        private String params;
    }
}
