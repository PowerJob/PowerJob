package com.github.kfcfans.oms.worker.pojo.model;

import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
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

    // 冗余存储一份 IP 地址
    private String ip;
    // 上次活跃时间
    private long lastActiveTime;
    // 等待执行任务数
    private long remainTaskNum;
    // 是否被派发过任务
    private boolean dispatched;

    public void init(String ip) {
        this.ip = ip;
        this.lastActiveTime = System.currentTimeMillis();
        this.remainTaskNum = 0;
        this.dispatched = false;
    }

    public void update(ProcessorTrackerStatusReportReq req) {

        // 延迟到达的请求，直接忽略
        if (req.getTime() <= lastActiveTime) {
            return;
        }

        this.ip = req.getIp();
        this.lastActiveTime = req.getTime();
        this.remainTaskNum = req.getRemainTaskNum();
        this.dispatched = true;
    }

    /**
     * 是否可用
     */
    public boolean available() {

        // 未曾派发过，默认可用
        if (!dispatched) {
            return true;
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
        return System.currentTimeMillis() - lastActiveTime > HEARTBEAT_TIMEOUT_MS;
    }
}
