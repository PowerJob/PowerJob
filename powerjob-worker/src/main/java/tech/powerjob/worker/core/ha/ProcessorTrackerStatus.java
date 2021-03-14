package tech.powerjob.worker.core.ha;

import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProcessorTracker 的状态
 *
 * @author tjq
 * @since 2020/3/27
 */
@Data
@NoArgsConstructor
public class ProcessorTrackerStatus {

    private static final int DISPATCH_THRESHOLD = 20;
    private static final int HEARTBEAT_TIMEOUT_MS = 60000;

    // 冗余存储一份 address 地址
    private String address;
    // 上次活跃时间
    private long lastActiveTime;
    // 等待执行任务数
    private long remainTaskNum;
    // 是否被派发过任务
    private boolean dispatched;
    // 是否接收到过来自 ProcessorTracker 的心跳
    private boolean connected;

    /**
     * 初始化 ProcessorTracker，此时并未持有实际的 ProcessorTracker 状态
     */
    public void init(String address) {
        this.address = address;
        this.lastActiveTime = - 1;
        this.remainTaskNum = 0;
        this.dispatched = false;
        this.connected = false;
    }

    /**
     * 接收到 ProcessorTracker 的心跳信息后，更新状态
     * @param req ProcessorTracker的心跳信息
     */
    public void update(ProcessorTrackerStatusReportReq req) {

        // 延迟到达的请求，直接忽略
        if (req.getTime() <= lastActiveTime) {
            return;
        }

        this.address = req.getAddress();
        this.lastActiveTime = req.getTime();
        this.remainTaskNum = req.getRemainTaskNum();
        this.dispatched = true;
        this.connected = true;
    }

    /**
     * 是否可用
     */
    public boolean available() {

        // 未曾派发过，默认可用
        if (!dispatched) {
            return true;
        }

        // 已派发但未收到响应，则不可用
        if (!connected) {
            return false;
        }

        // 长时间未收到心跳消息，则不可用
        if (isTimeout()) {
            return false;
        }

        // 留有过多待处理任务，则不可用
        if (remainTaskNum >= DISPATCH_THRESHOLD) {
            return false;
        }

        // TODO：后续考虑加上机器健康度等信息

        return true;
    }

    /**
     * 是否超时（超过一定时间没有收到心跳）
     */
    public boolean isTimeout() {
        if (dispatched) {
            return System.currentTimeMillis() - lastActiveTime > HEARTBEAT_TIMEOUT_MS;
        }
        // 未曾派发过任务的机器，不用处理
        return false;
    }

}
