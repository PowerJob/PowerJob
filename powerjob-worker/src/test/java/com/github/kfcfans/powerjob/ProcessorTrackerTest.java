package com.github.kfcfans.powerjob;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.utils.AkkaUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.powerjob.worker.pojo.model.InstanceInfo;
import com.github.kfcfans.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
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
public class ProcessorTrackerTest extends CommonTest {

    @Test
    public void testBasicProcessor() throws Exception {

        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("com.github.kfcfans.powerjob.processors.TestBasicProcessor");
        remoteProcessorTracker.tell(req, null);
        Thread.sleep(30000);
    }

    @Test
    public void testMapReduceProcessor() throws Exception {
        TaskTrackerStartTaskReq req = genTaskTrackerStartTaskReq("com.github.kfcfans.powerjob.processors.TestMapReduceProcessor");
        remoteProcessorTracker.tell(req, null);
        Thread.sleep(30000);
    }
}
