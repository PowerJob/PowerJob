package tech.powerjob.worker.test;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.common.utils.NetUtils;
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

        PowerJobWorkerConfig workerConfig = new PowerJobWorkerConfig();
        workerConfig.setAppName("oms-test");
        workerConfig.setServerAddress(Lists.newArrayList("127.0.0.1:7700"));
        workerConfig.setEnableTestMode(true);

        PowerJobWorker worker = new PowerJobWorker();
        worker.setConfig(workerConfig);
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
