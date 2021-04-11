package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import lombok.Data;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.SimpleUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;

/**
 * just sleep and wait time flies
 * eg, can use for workflow delay process
 *
 * @author KFC·D·Fans
 * @since 2021/4/11
 */
public class SleepProcessor extends CommonBasicProcessor {

    @Override
    protected ProcessResult process0(TaskContext ctx) throws Exception {

        Stopwatch sw = Stopwatch.createStarted();
        SleepParams sleepParams = JSONObject.parseObject(SimpleUtils.parseParams(ctx), SleepParams.class);

        if (sleepParams.sleepMillions != null) {
            CommonUtils.easySleep(sleepParams.sleepMillions);
        }

        return new ProcessResult(true, "total sleep: " + sw.toString());
    }

    @Data
    public static class SleepParams {
        private Long sleepMillions;
    }
}
