package tech.powerjob.worker.test;

import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.utils.NetUtils;
import com.google.common.collect.Lists;

/**
 * 测试需要用到的工具类
 *
 * @author tjq
 * @since 2020/4/9
 */
public class TestUtils {

    public static ServerScheduleJobReq genServerScheduleJobReq(ExecuteType executeType, TimeExpressionType timeExpressionType) {
        ServerScheduleJobReq req = new ServerScheduleJobReq();

        req.setJobId(1L);
        req.setInstanceId(10086L);
        req.setAllWorkerAddress(Lists.newArrayList(NetUtils.getLocalHost4Test() + ":" + RemoteConstant.DEFAULT_WORKER_PORT));

        req.setJobParams("JobParams");
        req.setInstanceParams("InstanceParams");
        req.setProcessorType(ProcessorType.BUILT_IN.name());
        req.setTaskRetryNum(3);
        req.setThreadConcurrency(10);
        req.setInstanceTimeoutMS(500000);
        req.setTimeExpressionType(timeExpressionType.name());
        switch (timeExpressionType) {
            case CRON:req.setTimeExpression("0 * * * * ? ");
            case FIXED_RATE:
            case FIXED_DELAY:req.setTimeExpression("5000");
        }

        switch (executeType) {
            case STANDALONE:
                req.setExecuteType(ExecuteType.STANDALONE.name());
                req.setProcessorInfo("tech.powerjob.worker.test.processors.TestBasicProcessor");
                break;
            case MAP_REDUCE:
                req.setExecuteType(ExecuteType.MAP_REDUCE.name());
                req.setProcessorInfo("tech.powerjob.worker.test.processors.TestMapReduceProcessor");
                break;
            case BROADCAST:
                req.setExecuteType(ExecuteType.BROADCAST.name());
                req.setProcessorInfo("tech.powerjob.worker.test.processors.TestBroadcastProcessor");
                break;
        }

        return req;
    }

}
