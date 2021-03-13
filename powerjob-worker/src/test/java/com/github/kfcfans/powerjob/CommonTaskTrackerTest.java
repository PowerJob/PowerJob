package com.github.kfcfans.powerjob;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.enums.ExecuteType;
import com.github.kfcfans.powerjob.common.enums.TimeExpressionType;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import com.github.kfcfans.powerjob.worker.common.utils.AkkaUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 测试完整的 JobInstance 执行流程
 *
 * @author tjq
 * @since 2020/3/25
 */
public class CommonTaskTrackerTest {

    private static ActorSelection remoteTaskTracker;

    @BeforeAll
    public static void init() throws Exception {

        OhMyConfig ohMyConfig = new OhMyConfig();
        ohMyConfig.setAppName("oms-test");
        ohMyConfig.setServerAddress(Lists.newArrayList("127.0.0.1:7700"));
        ohMyConfig.setEnableTestMode(true);

        OhMyWorker worker = new OhMyWorker();
        worker.setConfig(ohMyConfig);
        worker.init();

        ActorSystem testAS = ActorSystem.create("oms-test", ConfigFactory.load("oms-akka-test.conf"));
        String akkaRemotePath = AkkaUtils.getAkkaWorkerPath(NetUtils.getLocalHost() + ":" + RemoteConstant.DEFAULT_WORKER_PORT, RemoteConstant.TASK_TRACKER_ACTOR_NAME);
        remoteTaskTracker = testAS.actorSelection(akkaRemotePath);
    }

    @Test
    public void justStartWorkerToTestServer() throws Exception {
        Thread.sleep(277277277);
    }

    @Test
    public void testStandaloneJob() throws Exception {

        remoteTaskTracker.tell(TestUtils.genServerScheduleJobReq(ExecuteType.STANDALONE, TimeExpressionType.CRON), null);
        Thread.sleep(5000000);
    }

    @Test
    public void testMapReduceJob() throws Exception {
        remoteTaskTracker.tell(TestUtils.genServerScheduleJobReq(ExecuteType.MAP_REDUCE, TimeExpressionType.CRON), null);
        Thread.sleep(5000000);
    }

    @Test
    public void testBroadcast() throws Exception {
        remoteTaskTracker.tell(TestUtils.genServerScheduleJobReq(ExecuteType.BROADCAST, TimeExpressionType.CRON), null);
        Thread.sleep(5000000);
    }

}
