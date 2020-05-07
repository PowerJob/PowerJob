package com.github.kfcfans.oms.server.tester;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import com.github.kfcfans.oms.worker.log.OmsLogger;

/**
 * 测试 Oms 在线日志的性能
 *
 * @author tjq
 * @since 2020/5/3
 */
public class OmsLogPerformanceTester implements BasicProcessor {

    private static final int BATCH = 1000;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger logger = context.getOmsLogger();
        // 控制台参数，格式为 {"num":10000, "interval": 200}
        JSONObject jobParams = JSONObject.parseObject(context.getJobParams());
        Long num = jobParams.getLong("num");
        Long interval = jobParams.getLong("interval");

        RuntimeException re = new RuntimeException("This is a exception~~~");

        long times = num / BATCH;
        for (long i = 0; i < times; i++) {
            for (long j = 0; j < BATCH; j++) {
                long index = i * BATCH + j;
                System.out.println("send index: " + index);
                logger.info("[OmsLogPerformanceTester] testing omsLogger performance, current index is {}.", index);
            }
            logger.error("[OmsLogPerformanceTester] Oh, we have an exception to log~", re);
            try {
                Thread.sleep(interval);
            }catch (Exception ignore) {
            }
        }

        logger.info("[OmsLogPerformanceTester] success!");
        return new ProcessResult(true, "good job");
    }
}
