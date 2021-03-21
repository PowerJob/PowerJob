package tech.powerjob.worker.test;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * 启动公共服务
 *
 * @author tjq
 * @since 2020/6/17
 */
public class CommonTest {

    protected static ActorSelection remoteProcessorTracker;
    protected static ActorSelection remoteTaskTracker;

    @BeforeAll
    public static void startWorker() throws Exception {
        PowerJobWorkerConfig workerConfig = new PowerJobWorkerConfig();
        workerConfig.setAppName("oms-test");
        workerConfig.setEnableTestMode(true);

        PowerJobWorker worker = new PowerJobWorker();
        worker.setConfig(workerConfig);
        worker.init();

        ActorSystem testAS = ActorSystem.create("oms-test", ConfigFactory.load("oms-akka-test.conf"));
        String address = NetUtils.getLocalHost() + ":27777";

        remoteProcessorTracker = testAS.actorSelection(AkkaUtils.getAkkaWorkerPath(address, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME));
        remoteTaskTracker = testAS.actorSelection(AkkaUtils.getAkkaWorkerPath(address, RemoteConstant.TASK_TRACKER_ACTOR_NAME));
    }

    @AfterAll
    public static void stop() throws Exception {
        Thread.sleep(120000);
    }

    public static TaskTrackerStartTaskReq genTaskTrackerStartTaskReq(String processor) {

        InstanceInfo instanceInfo = new InstanceInfo();

        instanceInfo.setJobId(1L);
        instanceInfo.setInstanceId(10086L);

        instanceInfo.setExecuteType(ExecuteType.STANDALONE.name());
        instanceInfo.setProcessorType(ProcessorType.BUILT_IN.name());
        instanceInfo.setProcessorInfo(processor);

        instanceInfo.setInstanceTimeoutMS(500000);

        instanceInfo.setThreadConcurrency(5);
        instanceInfo.setTaskRetryNum(3);

        TaskTrackerStartTaskReq req = new TaskTrackerStartTaskReq();

        req.setTaskTrackerAddress(NetUtils.getLocalHost() + ":27777");
        req.setInstanceInfo(instanceInfo);

        req.setTaskId("0");
        req.setTaskName("ROOT_TASK");
        req.setTaskCurrentRetryNums(0);

        return req;
    }
}
