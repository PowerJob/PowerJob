package tech.powerjob.worker.actors;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.Handler;
import tech.powerjob.remote.framework.actor.ProcessType;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.core.tracker.manager.ProcessorTrackerManager;
import tech.powerjob.worker.core.tracker.processor.ProcessorTracker;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStopInstanceReq;

import java.util.List;

/**
 * 普通计算节点，处理来自 TaskTracker 的请求
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
@Actor(path = RemoteConstant.WPT_PATH)
public class ProcessorTrackerActor {

    private final WorkerRuntime workerRuntime;

    public ProcessorTrackerActor(WorkerRuntime workerRuntime) {
        this.workerRuntime = workerRuntime;
    }

    /**
     * 处理来自TaskTracker的task执行请求
     * @param req 请求
     */
    @Handler(path = RemoteConstant.WPT_HANDLER_START_TASK, processType = ProcessType.NO_BLOCKING)
    public void onReceiveTaskTrackerStartTaskReq(TaskTrackerStartTaskReq req) {

        Long instanceId = req.getInstanceInfo().getInstanceId();

        // 创建 ProcessorTracker 一定能成功
        ProcessorTracker processorTracker = ProcessorTrackerManager.getProcessorTracker(
                instanceId,
                req.getTaskTrackerAddress(),
                () -> new ProcessorTracker(req, workerRuntime));

        TaskDO task = new TaskDO();

        task.setTaskId(req.getTaskId());
        task.setTaskName(req.getTaskName());
        task.setTaskContent(req.getTaskContent());
        task.setFailedCnt(req.getTaskCurrentRetryNums());
        task.setSubInstanceId(req.getSubInstanceId());

        processorTracker.submitTask(task);
    }

    /**
     * 处理来自TaskTracker停止任务的请求
     * @param req 请求
     */
    @Handler(path = RemoteConstant.WPT_HANDLER_STOP_INSTANCE)
    public void onReceiveTaskTrackerStopInstanceReq(TaskTrackerStopInstanceReq req) {

        Long instanceId = req.getInstanceId();
        List<ProcessorTracker> removedPts = ProcessorTrackerManager.removeProcessorTracker(instanceId);
        if (!CollectionUtils.isEmpty(removedPts)) {
            removedPts.forEach(ProcessorTracker::destroy);
        }
    }
}
