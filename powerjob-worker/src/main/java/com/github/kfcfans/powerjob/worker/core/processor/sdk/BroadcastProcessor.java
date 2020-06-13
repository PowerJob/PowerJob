package com.github.kfcfans.powerjob.worker.core.processor.sdk;

import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.TaskResult;

import java.util.List;

/**
 * 广播执行处理器，适用于广播执行
 *
 * @author tjq
 * @since 2020/3/18
 */
public abstract class BroadcastProcessor implements BasicProcessor {

    /**
     * 在所有节点广播执行前执行，只会在一台机器执行一次
     */
    public ProcessResult preProcess(TaskContext context) throws Exception {
        return new ProcessResult(true);
    }
    /**
     * 在所有节点广播执行完成后执行，只会在一台机器执行一次
     */
    public ProcessResult postProcess(TaskContext context, List<TaskResult> taskResults) throws Exception {
        return defaultResult(taskResults);
    }

    public static ProcessResult defaultResult(List<TaskResult> taskResults) {
        long succeed = 0, failed = 0;
        for (TaskResult ts : taskResults) {
            if (ts.isSuccess()) {
                succeed ++ ;
            }else {
                failed ++;
            }
        }
        return new ProcessResult(succeed == 0, String.format("succeed:%d, failed:%d", succeed, failed));
    }
}
