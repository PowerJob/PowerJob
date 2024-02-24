package tech.powerjob.worker.pojo.converter;

import tech.powerjob.common.model.TaskDetailInfo;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.persistence.TaskDO;

/**
 * 任务相关的对象转换
 *
 * @author tjq
 * @since 2024/2/25
 */
public class TaskConverter {

    public static TaskDetailInfo taskDo2TaskDetail(TaskDO taskDO) {
        TaskDetailInfo taskDetailInfo = new TaskDetailInfo();
        taskDetailInfo.setTaskId(taskDO.getTaskId())
                .setTaskName(taskDO.getTaskName())
                .setStatus(taskDO.getStatus())
                .setStatusStr(TaskStatus.of(taskDetailInfo.getStatus()).name())
                .setResult(taskDO.getResult())
                .setFailedCnt(taskDO.getFailedCnt())
                .setProcessorAddress(taskDO.getAddress())
                .setCreatedTime(taskDO.getCreatedTime())
                .setLastModifiedTime(taskDO.getLastModifiedTime())
                .setLastReportTime(taskDO.getLastReportTime());
        return taskDetailInfo;
    }
}
