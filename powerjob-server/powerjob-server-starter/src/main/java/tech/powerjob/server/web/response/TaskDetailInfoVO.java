package tech.powerjob.server.web.response;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;
import tech.powerjob.common.model.TaskDetailInfo;
import tech.powerjob.common.utils.CommonUtils;

import java.io.Serializable;

/**
 * 任务详情
 *
 * @author tjq
 * @since 2024/2/25
 */
@Data
@Accessors(chain = true)
public class TaskDetailInfoVO implements Serializable {

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
    private String createdTimeStr;
    /**
     * 最后修改时间
     */
    private Long lastModifiedTime;
    private String lastModifiedTimeStr;
    /**
     * ProcessorTracker 最后上报时间
     */
    private Long lastReportTime;
    private String lastReportTimeStr;

    public static TaskDetailInfoVO from(TaskDetailInfo taskDetailInfo) {
        TaskDetailInfoVO vo = new TaskDetailInfoVO();
        BeanUtils.copyProperties(taskDetailInfo, vo);

        vo.setCreatedTimeStr(CommonUtils.formatTime(vo.getCreatedTime()));
        vo.setLastModifiedTimeStr(CommonUtils.formatTime(vo.getLastModifiedTime()));
        vo.setLastReportTimeStr(CommonUtils.formatTime(vo.getLastReportTime()));

        return vo;
    }
}
