package com.github.kfcfans.oms;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.common.AkkaConstant;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * 测试任务的启动
 *
 * @author tjq
 * @since 2020/3/24
 */
public class ProcessorTrackerTest {

    private static ActorSelection remoteProcessorTracker;

    @BeforeAll
    public static void startWorker() throws Exception {
        OhMyConfig ohMyConfig = new OhMyConfig();
        ohMyConfig.setAppName("oms-test");
        OhMyWorker worker = new OhMyWorker();
        worker.setConfig(ohMyConfig);
        worker.init();

        ActorSystem testAS = ActorSystem.create("oms-test", ConfigFactory.load("oms-akka-test.conf"));
        String akkaRemotePath = AkkaUtils.getAkkaRemotePath(NetUtils.getLocalHost(), AkkaConstant.PROCESSOR_TRACKER_ACTOR_NAME);
        remoteProcessorTracker = testAS.actorSelection(akkaRemotePath);
    }

    @AfterAll
    public static void stop() throws Exception {
        Thread.sleep(120000);
    }

    @Test
    public void testBasicProcessor() throws Exception {

        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("com.github.kfcfans.oms.processors.TestBasicProcessor");
        remoteProcessorTracker.tell(req, null);
        Thread.sleep(30000);
    }

    @Test
    public void testMapReduceProcessor() throws Exception {
        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("com.github.kfcfans.oms.processors.TestMapReduceProcessor");
        remoteProcessorTracker.tell(req, null);
        Thread.sleep(30000);
    }

    private static TaskTrackerStartTaskReq genTaskTrackerStartTaskReq(String processor) {
        TaskTrackerStartTaskReq req = new TaskTrackerStartTaskReq();
        req.setJobId("1");
        req.setInstanceId("10086");
        req.setTaskId("0");
        req.setTaskName("ROOT_TASK");
        req.setMaxRetryTimes(3);
        req.setCurrentRetryTimes(0);

        req.setExecuteType(ExecuteType.STANDALONE.name());
        req.setProcessorType(ProcessorType.EMBEDDED_JAVA.name());
        req.setProcessorInfo(processor);
        req.setThreadConcurrency(5);
        req.setTaskTrackerAddress(NetUtils.getLocalHost());
        req.setJobTimeLimitMS(123132);

        return req;
    }
}
