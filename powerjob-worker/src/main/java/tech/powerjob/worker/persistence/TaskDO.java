package tech.powerjob.worker.persistence;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * TaskDO（为了简化 DAO 层，一张表实现两种功能）
 * 对于 TaskTracker，task_info 存储了当前 JobInstance 所有的子任务信息
 * 对于普通的 Worker，task_info 存储了当前无法处理的任务信息
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@Setter
public class TaskDO {

    /**
     * 层次命名法，可以表示 Map 后的父子关系，如 0.1.2 代表 rootTask map 的第一个 task map 的第二个 task
     */
    private String taskId;
    /**
     * 任务实例 ID
     */
    private Long instanceId;
    /**
     * 秒级任务专用
     * 对于普通任务而言 等于 instanceId
     * 对于秒级（固定频率）任务 自增长
     */
    private Long subInstanceId;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     *  任务对象（序列化后的二进制数据）
     */
    private byte[] taskContent;
    /**
     * 对于TaskTracker为workerAddress（派发地址），对于普通Worker为TaskTrackerAddress（汇报地址），所有地址都是 IP:Port
     */
    private String address;
    /**
     * 任务状态，0～10代表 JobTracker 使用，11～20代表普通Worker使用
     */
    private Integer status;
    /**
     * 执行结果
     */
    private String result;
    /**
     * 失败次数
     */
    private Integer failedCnt;
    /**
     * 创建时间
     */
    private Long createdTime;
    /**
     * 最后修改时间
     */
    private Long lastModifiedTime;
    /**
     * ProcessorTracker 最后上报时间
     */
    private Long lastReportTime;

    public String fetchUpdateSQL() {
        StringBuilder sb = new StringBuilder();

        // address 有置空需求，仅判断 NULL
        if (address != null) {
            sb.append(" address = '").append(address).append("',");
        }
        if (status != null) {
            sb.append(" status = ").append(status).append(",");
        }
        if (!StringUtils.isEmpty(result)) {
            sb.append(" result = '").append(result).append("',");
        }
        if (failedCnt != null) {
            sb.append(" failed_cnt = ").append(failedCnt).append(",");
        }
        if (lastReportTime != null) {
            sb.append(" last_report_time = ").append(lastReportTime).append(",");
        }
        sb.append(" last_modified_time = ").append(lastModifiedTime == null ? System.currentTimeMillis() : lastModifiedTime);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "{" +
                "taskId='" + taskId + '\'' +
                ", instanceId=" + instanceId +
                ", subInstanceId=" + subInstanceId +
                ", taskName='" + taskName + '\'' +
                ", address='" + address + '\'' +
                ", status=" + status +
                ", result='" + result + '\'' +
                ", failedCnt=" + failedCnt +
                ", createdTime=" + createdTime +
                ", lastModifiedTime=" + lastModifiedTime +
                ", lastReportTime=" + lastReportTime +
                '}';
    }
}
