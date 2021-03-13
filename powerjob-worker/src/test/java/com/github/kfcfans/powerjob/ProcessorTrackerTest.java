package com.github.kfcfans.powerjob;

import com.github.kfcfans.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
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
