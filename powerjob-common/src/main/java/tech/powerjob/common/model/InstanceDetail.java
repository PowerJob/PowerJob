package tech.powerjob.common.model;

import tech.powerjob.common.PowerSerializable;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     */
    private TaskDetail taskDetail;
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
    }
}
