package tech.powerjob.worker.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.core.tracker.processor.ProcessorTracker;
import tech.powerjob.worker.core.tracker.processor.ProcessorTrackerPool;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStopInstanceReq;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 普通计算节点，处理来自 TaskTracker 的请求
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
@AllArgsConstructor
public class ProcessorTrackerActor extends AbstractActor {

    private final WorkerRuntime workerRuntime;

    public static Props props(WorkerRuntime workerRuntime) {
        return Props.create(ProcessorTrackerActor.class, () -> new ProcessorTrackerActor(workerRuntime));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TaskTrackerStartTaskReq.class, this::onReceiveTaskTrackerStartTaskReq)
                .match(TaskTrackerStopInstanceReq.class, this::onReceiveTaskTrackerStopInstanceReq)
                .matchAny(obj -> log.warn("[ProcessorTrackerActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理来自TaskTracker的task执行请求
     * @param req 请求
     */
    private void onReceiveTaskTrackerStartTaskReq(TaskTrackerStartTaskReq req) {

        Long instanceId = req.getInstanceInfo().getInstanceId();

        // 创建 ProcessorTracker 一定能成功
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(
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
    private void onReceiveTaskTrackerStopInstanceReq(TaskTrackerStopInstanceReq req) {

        Long instanceId = req.getInstanceId();
        List<ProcessorTracker> removedPts = ProcessorTrackerPool.removeProcessorTracker(instanceId);
        if (!CollectionUtils.isEmpty(removedPts)) {
            removedPts.forEach(ProcessorTracker::destroy);
        }
    }
}
