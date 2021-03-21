package tech.powerjob.worker.core.processor.sdk;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;

import java.util.List;

/**
 * 广播执行处理器，适用于广播执行
 *
 * @author tjq
 * @since 2020/3/18
 */
public interface BroadcastProcessor extends BasicProcessor {

    /**
     * 在所有节点广播执行前执行，只会在一台机器执行一次
     */
    default ProcessResult preProcess(TaskContext context) throws Exception {
        return new ProcessResult(true);
    }
    /**
     * 在所有节点广播执行完成后执行，只会在一台机器执行一次
     */
    default ProcessResult postProcess(TaskContext context, List<TaskResult> taskResults) throws Exception {
        return defaultResult(taskResults);
    }

    static ProcessResult defaultResult(List<TaskResult> taskResults) {
        long succeed = 0, failed = 0;
        for (TaskResult ts : taskResults) {
            if (ts.isSuccess()) {
                succeed ++ ;
            }else {
                failed ++;
            }
        }
        return new ProcessResult(failed == 0, String.format("succeed:%d, failed:%d", succeed, failed));
    }
}
