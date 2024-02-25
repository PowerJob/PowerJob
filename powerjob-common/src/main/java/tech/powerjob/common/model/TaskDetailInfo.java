package tech.powerjob.common.model;

import lombok.Data;
import lombok.experimental.Accessors;
import tech.powerjob.common.PowerSerializable;

/**
 * Task 详细信息
 *
 * @author tjq
 * @since 2024/2/25
 */
@Data
@Accessors(chain = true)
public class TaskDetailInfo implements PowerSerializable {

    private String taskId;
    private String taskName;
    /**
     * 任务对象（map 的 subTask）
     */
    private String taskContent;
    /**
     * 处理器地址
     */
    private String processorAddress;
    private Integer status;
    private String statusStr;
    private String result;
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
}
