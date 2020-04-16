package com.github.kfcfans.oms.processors.demo;

import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BroadcastProcessor;

import java.util.Map;

/**
 * 示例-广播执行处理器
 *
 * @author tjq
 * @since 2020/4/15
 */
public class BroadcastProcessorDemo implements BroadcastProcessor {

    @Override
    public ProcessResult preProcess(TaskContext taskContext) throws Exception {
        // 预执行，会在所有 worker 执行 process 方法前调用
        return new ProcessResult(true, "init success");
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        // 撰写整个worker集群都会执行的代码逻辑
        return new ProcessResult(true, "release resource success");
    }

    @Override
    public ProcessResult postProcess(TaskContext taskContext, Map<String, String> taskId2Result) throws Exception {

        // taskId2Result 存储了所有worker执行的结果（包括preProcess）

        // 收尾，会在所有 worker 执行完毕 process 方法后调用
        return new ProcessResult(true, "process success");
    }

}
