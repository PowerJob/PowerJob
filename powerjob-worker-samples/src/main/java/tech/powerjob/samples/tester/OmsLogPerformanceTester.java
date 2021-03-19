package tech.powerjob.samples.tester;

import com.alibaba.fastjson.JSONObject;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;
import org.springframework.stereotype.Component;

/**
 * 测试 Oms 在线日志的性能
 *
 * @author tjq
 * @since 2020/5/3
 */
@Component
public class OmsLogPerformanceTester implements BasicProcessor {

    private static final int BATCH = 1000;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger omsLogger = context.getOmsLogger();
        // 控制台参数，格式为 {"num":10000, "interval": 200}
        JSONObject jobParams = JSONObject.parseObject(context.getJobParams());
        Long num = jobParams.getLong("num");
        Long interval = jobParams.getLong("interval");

        omsLogger.info("ready to start to process, current JobParams is {}.", jobParams);

        RuntimeException re = new RuntimeException("This is a exception~~~");

        long times = (long) Math.ceil(1.0 * num / BATCH);
        for (long i = 0; i < times; i++) {
            for (long j = 0; j < BATCH; j++) {
                long index = i * BATCH + j;
                System.out.println("send index: " + index);

                omsLogger.info("testing omsLogger's performance, current index is {}.", index);
            }
            omsLogger.error("Oh, it seems that we have got an exception.", re);
            try {
                Thread.sleep(interval);
            }catch (Exception ignore) {
            }
        }

        omsLogger.info("anyway, we finished the job~configuration~");
        return new ProcessResult(true, "good job");
    }
}
