package com.github.kfcfans.oms;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.github.kfcfans.common.AkkaConstant;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 测试完整的 JobInstance 执行流程
 *
 * @author tjq
 * @since 2020/3/25
 */
public class TaskTrackerTest {

    private static ActorSelection remoteTaskTracker;

    @BeforeAll
    public static void init() {

        OhMyConfig ohMyConfig = new OhMyConfig();
        ohMyConfig.setAppName("oms-test");
        OhMyWorker worker = new OhMyWorker();
        worker.setConfig(ohMyConfig);
        worker.init();

        ActorSystem testAS = ActorSystem.create("oms-test", ConfigFactory.load("oms-akka-test.conf"));
        String akkaRemotePath = AkkaUtils.getAkkaRemotePath(NetUtils.getLocalHost(), AkkaConstant.Task_TRACKER_ACTOR_NAME);
        remoteTaskTracker = testAS.actorSelection(akkaRemotePath);
    }

    @Test
    public void testStandaloneJob() throws Exception {

        remoteTaskTracker.tell(genServerScheduleJobReq(ExecuteType.STANDALONE), null);
        Thread.sleep(500000);
    }

    @Test
    public void testMapReduceJob() throws Exception {
        remoteTaskTracker.tell(genServerScheduleJobReq(ExecuteType.MAP_REDUCE), null);
        Thread.sleep(500000);
    }

    private static ServerScheduleJobReq genServerScheduleJobReq(ExecuteType executeType) {
        ServerScheduleJobReq req = new ServerScheduleJobReq();

        req.setJobId("1");
        req.setInstanceId("10086");
        req.setAllWorkerAddress(NetUtils.getLocalHost());

        req.setJobParams("this is job Params");
        req.setInstanceParams("this is instance Params");
        req.setProcessorType(ProcessorType.EMBEDDED_JAVA.name());
        req.setTaskRetryNum(3);
        req.setThreadConcurrency(20);
        req.setInstanceTimeoutMS(500000);
        req.setTaskTimeoutMS(500000);

        switch (executeType) {
            case STANDALONE:
                req.setExecuteType(ExecuteType.STANDALONE.name());
                req.setProcessorInfo("com.github.kfcfans.oms.processors.TestBasicProcessor");
                break;
            case MAP_REDUCE:
                req.setExecuteType(ExecuteType.MAP_REDUCE.name());
                req.setProcessorInfo("com.github.kfcfans.oms.processors.TestMapReduceProcessor");
                break;
            case BROADCAST:
                req.setExecuteType(ExecuteType.BROADCAST.name());
                req.setProcessorInfo("com.github.kfcfans.oms.processors.TestBroadcastProcessor");
                break;
        }

        return req;
    }


}
