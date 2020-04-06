package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.common.utils.NetUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProcessorTracker 定时向 TaskTracker 上报健康状态
 *
 * @author tjq
 * @since 2020/3/27
 */
@Data
@NoArgsConstructor
public class ProcessorTrackerStatusReportReq {

    private Long instanceId;

    /**
     * 请求发起时间
     */
    private long time;

    /**
     * 等待执行的任务数量，内存队列数 + 数据库持久数
     */
    private long remainTaskNum;

    /**
     * 本机地址
     */
    private String ip;

    public ProcessorTrackerStatusReportReq(Long instanceId, long remainTaskNum) {
        this.instanceId = instanceId;
        this.remainTaskNum = remainTaskNum;

        this.time = System.currentTimeMillis();
        this.ip = NetUtils.getLocalHost();
    }
}
