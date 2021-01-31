package tech.powerjob.official.processors;

import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * CommonBasicProcessor
 *
 * @author tjq
 * @since 2021/1/30
 */
public abstract class CommonBasicProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext taskContext) throws Exception {

        String clzName = this.getClass().getSimpleName();
        OmsLogger omsLogger = taskContext.getOmsLogger();
        omsLogger.info("[{}] using params: {}", clzName, taskContext.getJobParams());

        try {
            Stopwatch sw = Stopwatch.createStarted();
            ProcessResult result = process0(taskContext);
            omsLogger.info("[{}] execute succeed, using {}, result: {}", clzName, sw, result);
            return suit(result);
        } catch (Throwable t) {
            omsLogger.error("[{}] execute failed!", clzName, t);
            return new ProcessResult(false, ExceptionUtils.getMessage(t));
        }
    }

    private static ProcessResult suit(ProcessResult processResult) {
        if (processResult.getMsg() == null || processResult.getMsg().length() < 1024) {
            return processResult;
        }
        processResult.setMsg(processResult.getMsg().substring(0, 1024) + "...");
        return processResult;
    }

    protected abstract ProcessResult process0(TaskContext taskContext) throws Exception;
}

