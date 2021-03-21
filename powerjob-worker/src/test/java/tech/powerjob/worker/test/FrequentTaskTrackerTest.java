package tech.powerjob.worker.test;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.utils.AkkaUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * description
 *
 * @author tjq
 * @since 2020/4/9
 */
public class FrequentTaskTrackerTest {

    private static ActorSelection remoteTaskTracker;

    @BeforeAll
    public static void init() throws Exception {

        PowerJobWorkerConfig workerConfig = new PowerJobWorkerConfig();
        workerConfig.setAppName("oms-test");
        workerConfig.setServerAddress(Lists.newArrayList("127.0.0.1:7700"));
        PowerJobWorker worker = new PowerJobWorker();
        worker.setConfig(workerConfig);
        worker.init();

        ActorSystem testAS = ActorSystem.create("oms-test", ConfigFactory.load("oms-akka-test.conf"));
        String akkaRemotePath = AkkaUtils.getAkkaWorkerPath(NetUtils.getLocalHost() + ":" + RemoteConstant.DEFAULT_WORKER_PORT, RemoteConstant.TASK_TRACKER_ACTOR_NAME);
        remoteTaskTracker = testAS.actorSelection(akkaRemotePath);
    }

    @Test
    public void testFixRateJob() throws Exception {
        remoteTaskTracker.tell(TestUtils.genServerScheduleJobReq(ExecuteType.STANDALONE, TimeExpressionType.FIXED_RATE), null);
        Thread.sleep(5000000);
    }

    @Test
    public void testFixDelayJob() throws Exception {
        remoteTaskTracker.tell(TestUtils.genServerScheduleJobReq(ExecuteType.MAP_REDUCE, TimeExpressionType.FIXED_DELAY), null);
        Thread.sleep(5000000);
    }
}
