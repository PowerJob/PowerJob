package tech.powerjob.worker.core.processor.sdk;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;

import java.util.List;

/**
 * MapReduce执行处理器，适用于MapReduce任务
 * 在 MapProcessor 的基础上添加了结果汇集（reduce）的方法
 *
 * @author tjq
 * @since 2020/3/18
 */
public interface MapReduceProcessor extends MapProcessor {

    /**
     * reduce方法将在所有任务结束后调用
     * @param context 任务上下文
     * @param taskResults 保存了各个子Task的执行结果
     * @return reduce产生的结果将作为任务最终的返回结果
     */
    ProcessResult reduce(TaskContext context, List<TaskResult> taskResults);
}
