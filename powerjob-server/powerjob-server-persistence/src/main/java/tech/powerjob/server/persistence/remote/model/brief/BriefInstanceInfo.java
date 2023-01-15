package tech.powerjob.server.persistence.remote.model.brief;


import lombok.Data;

/**
 * @author Echo009
 * @since 2022/9/13
 */
@Data
public class BriefInstanceInfo {


    private Long appId;

    private Long id;
    /**
     * 任务ID
     */
    private Long jobId;
    /**
     * 任务所属应用的ID，冗余提高查询效率
     */
    private Long instanceId;
    /**
     * 总共执行的次数（用于重试判断）
     */
    private Long runningTimes;


    public BriefInstanceInfo(Long appId, Long id, Long jobId, Long instanceId) {
        this.appId = appId;
        this.id = id;
        this.jobId = jobId;
        this.instanceId = instanceId;
    }

    public BriefInstanceInfo(Long appId, Long id, Long jobId, Long instanceId, Long runningTimes) {
        this.appId = appId;
        this.id = id;
        this.jobId = jobId;
        this.instanceId = instanceId;
        this.runningTimes = runningTimes;
    }
}
