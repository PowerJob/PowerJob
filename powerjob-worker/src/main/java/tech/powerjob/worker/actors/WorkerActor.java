package tech.powerjob.worker.actors;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.request.*;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.container.OmsContainerFactory;

import static tech.powerjob.common.RemoteConstant.*;

/**
 * Worker节点Actor，接受服务器请求
 *
 * @author tjq
 * @since 2020/3/24
 */
@Slf4j
@Actor(path = WORKER_PATH)
public class WorkerActor {

    private final WorkerRuntime workerRuntime;
    private final TaskTrackerActor taskTrackerActor;


    public WorkerActor(WorkerRuntime workerRuntime, TaskTrackerActor taskTrackerActor) {
        this.workerRuntime = workerRuntime;
        this.taskTrackerActor = taskTrackerActor;
    }

    @Handler(path = WORKER_HANDLER_DEPLOY_CONTAINER)
    public void onReceiveServerDeployContainerRequest(ServerDeployContainerRequest request) {
        OmsContainerFactory.deployContainer(request);
    }

    @Handler(path = WORKER_HANDLER_DESTROY_CONTAINER)
    public void onReceiveServerDestroyContainerRequest(ServerDestroyContainerRequest request) {
        OmsContainerFactory.destroyContainer(request.getContainerId());
    }

    @Handler(path = WTT_HANDLER_RUN_JOB)
    public void onReceiveServerScheduleJobReq(ServerScheduleJobReq req) {
        taskTrackerActor.onReceiveServerScheduleJobReq(req);
    }
    @Handler(path = WTT_HANDLER_STOP_INSTANCE)
    public void onReceiveServerStopInstanceReq(ServerStopInstanceReq req) {
        taskTrackerActor.onReceiveServerStopInstanceReq(req);
    }
    @Handler(path = WTT_HANDLER_QUERY_INSTANCE_STATUS)
    public AskResponse onReceiveServerQueryInstanceStatusReq(ServerQueryInstanceStatusReq req) {
        return taskTrackerActor.onReceiveServerQueryInstanceStatusReq(req);
    }
}
