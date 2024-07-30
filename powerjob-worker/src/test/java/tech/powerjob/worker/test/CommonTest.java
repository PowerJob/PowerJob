package tech.powerjob.worker.test;

import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;

/**
 * 启动公共服务
 *
 * @author tjq
 * @since 2020/6/17
 */
public class CommonTest {


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

        req.setTaskTrackerAddress(NetUtils.getLocalHost4Test() + ":27777");
        req.setInstanceInfo(instanceInfo);

        req.setTaskId("0");
        req.setTaskName("ROOT_TASK");
        req.setTaskCurrentRetryNums(0);

        return req;
    }
}
