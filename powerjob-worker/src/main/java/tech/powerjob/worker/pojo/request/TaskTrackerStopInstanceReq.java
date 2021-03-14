package tech.powerjob.worker.pojo.request;

import tech.powerjob.common.PowerSerializable;
import lombok.Data;


/**
 * TaskTracker 停止 ProcessorTracker，释放相关资源
 * 任务执行完毕后停止 OR 手动强制停止
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class TaskTrackerStopInstanceReq implements PowerSerializable {

    private Long instanceId;
    // 保留字段，暂时没用
    private String type;
}
