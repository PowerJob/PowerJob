package tech.powerjob.official.processors;

import tech.powerjob.official.processors.util.SecurityUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;
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

        OmsLogger omsLogger = ctx.getOmsLogger();
        String securityDKey = getSecurityDKey();
        if (SecurityUtils.disable(securityDKey)) {
            String msg = String.format("%s is not enabled, please set '-D%s=true' to enable it", this.getClass().getSimpleName(), securityDKey);
            omsLogger.warn(msg);
            return new ProcessResult(false, msg);
        }

        String status = "unknown";
        Stopwatch sw = Stopwatch.createStarted();

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

    protected String getSecurityDKey() {
        return null;
    }
}

