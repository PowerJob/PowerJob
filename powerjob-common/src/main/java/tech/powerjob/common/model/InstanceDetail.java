package tech.powerjob.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.powerjob.common.PowerSerializable;

import java.util.List;

/**
 * Detailed info of job instances.
 *
 * @author tjq
 * @since 2020/4/11
 */
@Data
@NoArgsConstructor
public class InstanceDetail implements PowerSerializable {

    /**
     * Expected trigger time.
     */
    private Long expectedTriggerTime;
    /**
     * Actual trigger time of an instance.
     */
    private Long actualTriggerTime;
    /**
     * Finish time of an instance, which may be null.
     */
    private Long finishedTime;
    /**
     * Status of the task instance.
     */
    private Integer status;
    /**
     * Execution result, which may be null.
     */
    private String result;
    /**
     * Task tracker address.
     */
    private String taskTrackerAddress;
    /**
     * 任务参数
     */
    private String jobParams;
    /**
     * Param string that is passed to an instance when it is initialized.
     */
    private String instanceParams;

    /**
     * Task detail, used by MapReduce or Broadcast tasks.
     * 命名有点问题，实际是 task 统计信息
     */
    private TaskDetail taskDetail;

    /**
     * 查询出来的 Task 详细结果
     */
    private List<TaskDetailInfo> queriedTaskDetailInfoList;

    /**
     * Sub instance details, used by frequent tasks.
     */
    private List<SubInstanceDetail> subInstanceDetails;

    /**
     * Running times.
     */
    private Long runningTimes;

    /**
     * Extended fields. Middlewares are not supposed to update frequently.
     * Changes in PowerJob-common may lead to incompatible versions.
     * PowerJob-common packages should not be modified if not necessary.
     */
    private String extra;

    /**
     * Extra info for frequent tasks, return List<SubInstanceDetail>.
     */
    @Data
    @NoArgsConstructor
    public static class SubInstanceDetail implements PowerSerializable {
        private long subInstanceId;
        private Long startTime;
        private Long finishedTime;
        private String result;
        private int status;
    }

    /**
     * Extra info of {@code MapReduce} or {@code Broadcast} type of tasks.
     */
    @Data
    @NoArgsConstructor
    public static class TaskDetail implements PowerSerializable {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;

        // 等待派发状态（仅存在 TaskTracker 数据库中）
        protected Long waitingDispatchTaskNum;
        // 已派发，但 ProcessorTracker 未确认，可能由于网络错误请求未送达，也有可能 ProcessorTracker 线程池满，拒绝执行
        protected Long workerUnreceivedTaskNum;
        // ProcessorTracker确认接收，存在与线程池队列中，排队执行
        protected Long receivedTaskNum;
        // ProcessorTracker正在执行
        protected Long runningTaskNum;
    }
}
