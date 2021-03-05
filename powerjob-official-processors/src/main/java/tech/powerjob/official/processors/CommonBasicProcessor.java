package tech.powerjob.official.processors;

import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.official.processors.util.CommonUtils;

/**
 * CommonBasicProcessor
 *
 * @author tjq
 * @since 2021/1/30
 */
@Slf4j
public abstract class CommonBasicProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext ctx) throws Exception {

        String status = "unknown";
        Stopwatch sw = Stopwatch.createStarted();

        OmsLogger omsLogger = ctx.getOmsLogger();
        omsLogger.info("using params: {}", CommonUtils.parseParams(ctx));

        try {
            ProcessResult result = process0(ctx);
            omsLogger.info("execute succeed, using {}, result: {}", sw, result);
            status = result.isSuccess() ? "succeed" : "failed";
            return result;
        } catch (Throwable t) {
            status = "exception";
            omsLogger.error("execute failed!", t);
            return new ProcessResult(false, ExceptionUtils.getMessage(t));
        } finally {
            log.info("{}|{}|{}|{}|{}", getClass().getSimpleName(), ctx.getJobId(), ctx.getInstanceId(), status, sw);
        }
    }

    protected abstract ProcessResult process0(TaskContext taskContext) throws Exception;
}

