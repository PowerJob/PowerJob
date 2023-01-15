package tech.powerjob.server.persistence.remote.model;

import tech.powerjob.common.enums.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 任务运行日志表
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx01_instance_info", columnList = "jobId,status"),
        @Index(name = "idx02_instance_info", columnList = "appId,status"),
        @Index(name = "idx03_instance_info", columnList = "instanceId,status")
})
public class InstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    /**
     * 任务ID
     */
    private Long jobId;
    /**
     * 任务所属应用的ID，冗余提高查询效率
     */
    private Long appId;
    /**
     * 任务所属应用的ID，冗余提高查询效率
     */
    private Long instanceId;
    /**
     * 任务参数（静态）
     *
     * @since 2021/2/01
     */
    @Lob
    @Column
    private String jobParams;
    /**
     * 任务实例参数（动态）
     */
    @Lob
    @Column
    private String instanceParams;
    /**
     * 该任务实例的类型，普通/工作流（InstanceType）
     */
    private Integer type;
    /**
     * 该任务实例所属的 workflow ID，仅 workflow 任务存在
     */
    private Long wfInstanceId;
    /**
     * 任务状态 {@link InstanceStatus}
     */
    private Integer status;
    /**
     * 执行结果（允许存储稍大的结果）
     */
    @Lob
    @Column
    private String result;
    /**
     * 预计触发时间
     */
    private Long expectedTriggerTime;
    /**
     * 实际触发时间
     */
    private Long actualTriggerTime;
    /**
     * 结束时间
     */
    private Long finishedTime;
    /**
     * 最后上报时间
     */
    private Long lastReportTime;
    /**
     * TaskTracker 地址
     */
    private String taskTrackerAddress;
    /**
     * 总共执行的次数（用于重试判断）
     */
    private Long runningTimes;


    private Date gmtCreate;

    private Date gmtModified;

}
