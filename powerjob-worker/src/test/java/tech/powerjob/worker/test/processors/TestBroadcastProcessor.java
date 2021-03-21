package tech.powerjob.worker.test.processors;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;

import java.util.List;

/**
 * 测试用的广播执行处理器
 *
 * @author tjq
 * @since 2020/3/25
 */
public class TestBroadcastProcessor implements BroadcastProcessor {
    @Override
    public ProcessResult preProcess(TaskContext taskContext) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#preProcess ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(taskContext));
        return new ProcessResult(true, "preProcess success");
    }

    @Override
    public ProcessResult postProcess(TaskContext taskContext, List<TaskResult> taskResults) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#postProcess ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(taskContext));
        System.out.println("taskId2Result:" + taskResults);
        return new ProcessResult(true, "postProcess success");
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#process ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));
        return new ProcessResult(true, "processSuccess");
    }
}
