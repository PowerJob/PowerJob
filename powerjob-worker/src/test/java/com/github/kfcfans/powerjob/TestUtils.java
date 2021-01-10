package com.github.kfcfans.powerjob;

import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.ServerScheduleJobReq;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
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
        req.setAllWorkerAddress(Lists.newArrayList(NetUtils.getLocalHost() + ":" + RemoteConstant.DEFAULT_WORKER_PORT));

        req.setJobParams("JobParams");
        req.setInstanceParams("InstanceParams");
        req.setProcessorType(ProcessorType.EMBEDDED_JAVA.name());
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
                req.setProcessorInfo("com.github.kfcfans.powerjob.processors.TestBasicProcessor");
                break;
            case MAP_REDUCE:
                req.setExecuteType(ExecuteType.MAP_REDUCE.name());
                req.setProcessorInfo("com.github.kfcfans.powerjob.processors.TestMapReduceProcessor");
                break;
            case BROADCAST:
                req.setExecuteType(ExecuteType.BROADCAST.name());
                req.setProcessorInfo("com.github.kfcfans.powerjob.processors.TestBroadcastProcessor");
                break;
        }

        return req;
    }

}
