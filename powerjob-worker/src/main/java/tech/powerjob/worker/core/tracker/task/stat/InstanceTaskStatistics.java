package tech.powerjob.worker.core.tracker.task.stat;

import lombok.Data;

import java.io.Serializable;

/**
 * 存储任务实例产生的各个Task状态，用于分析任务实例执行情况
 *
 * @author tjq
 * @since 2024/2/21
 */
@Data
public class InstanceTaskStatistics implements Serializable {

    /**
     * 等待派发状态（仅存在 TaskTracker 数据库中）
     */
    private long waitingDispatchNum;
    /**
     * 已派发，但 ProcessorTracker 未确认，可能由于网络错误请求未送达，也有可能 ProcessorTracker 线程池满，拒绝执行
     */
    private long workerUnreceivedNum;
    /**
     * ProcessorTracker确认接收，存在与线程池队列中，排队执行
     */
    private long receivedNum;
    /**
     * ProcessorTracker正在执行
     */
    private long runningNum;
    private long failedNum;
    private long succeedNum;

    public long getTotalTaskNum() {
        return waitingDispatchNum + workerUnreceivedNum + receivedNum + runningNum + failedNum + succeedNum;
    }

    public long getFinishedNum() {
        return succeedNum + failedNum;
    }

    public long getUnfinishedNum() {
        return getTotalTaskNum() - getFinishedNum();
    }
}
