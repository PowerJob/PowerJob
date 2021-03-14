package tech.powerjob.worker.pojo.request;

import tech.powerjob.common.PowerSerializable;
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
public class ProcessorTrackerStatusReportReq implements PowerSerializable {

    public static final int IDLE = 1;
    public static final int LOAD = 2;

    // IDLE 代表 ProcessorTracker 长期处于空闲状态，LOAD 代表 负载上报请求
    private int type;

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
    private String address;


    public static ProcessorTrackerStatusReportReq buildIdleReport(Long instanceId) {
        ProcessorTrackerStatusReportReq req = new ProcessorTrackerStatusReportReq();
        req.type = IDLE;
        req.instanceId = instanceId;
        req.time = System.currentTimeMillis();
        req.setRemainTaskNum(0);
        return req;
    }

    public static ProcessorTrackerStatusReportReq buildLoadReport(Long instanceId, Long remainTaskNum) {
        ProcessorTrackerStatusReportReq req = new ProcessorTrackerStatusReportReq();
        req.type = LOAD;
        req.instanceId = instanceId;
        req.time = System.currentTimeMillis();
        req.setRemainTaskNum(remainTaskNum);
        return req;
    }
}
