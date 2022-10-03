package tech.powerjob.common.request.http;

import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.AlarmConfig;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.model.LifeCycle;
import tech.powerjob.common.utils.CommonUtils;
import lombok.Data;
import tech.powerjob.common.response.JobInfoDTO;

import java.util.List;

/**
 * Save or modify {@link JobInfoDTO}
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
public class SaveJobInfoRequest {

    /**
     * id of the job. set null to create or non-null to update the job.
     */
    private Long id;
    /* ************************** Base info of related job. ************************** */

    /**
     * Name of the job.
     */
    private String jobName;
    /**
     * Description of the job.
     */
    private String jobDescription;
    /**
     * Related id of the application. There is not need to set this property
     * in PowerJob-client, as it would be set automatically.
     */
    private Long appId;
    /**
     * Params that these jobs carry with when they are created.
     */
    private String jobParams;

    /* ************************** Timing param. ************************** */
    /**
     * Time expression type.
     */
    private TimeExpressionType timeExpressionType;
    /**
     * Time expression.
     */
    private String timeExpression;


    /* ************************** Execution type. ************************** */
    /**
     * Execution type, {@code standalone}, {@code broadcast} or {@code Map/MapReduce}
     */
    private ExecuteType executeType;
    /**
     * Processor type, {@code Java}, {@code Python} or {@code Shell}.
     */
    private ProcessorType processorType;
    /**
     * Processor info.
     */
    private String processorInfo;


    /* ************************** Running instance setting. ************************** */
    /**
     * Maximum instance setting num. {@code 0} means there is no restriction.
     */
    private Integer maxInstanceNum = 0;
    /**
     * Concurrency setting. Number of threads that run simultaneously.
     */
    private Integer concurrency = 5;
    /**
     * Max instance running time setting. {@code 0L} means there is no restriction.
     */
    private Long instanceTimeLimit = 0L;

    /* ************************** Retrial setting. ************************** */
    /**
     * Instance retry number setting.
     */
    private Integer instanceRetryNum = 0;
    /**
     * Task retry number setting.
     */
    private Integer taskRetryNum = 0;

    /* ************************** Busy Machine setting. ************************** */
    /**
     * Minimum CPU required. {@code 0} means there is no restriction.
     */
    private double minCpuCores = 0;
    /**
     * Minimum memory required, in GB.
     */
    private double minMemorySpace = 0;
    /**
     * Minimum disk space, in GB. {@code 0} means there is no restriction.
     */
    private double minDiskSpace = 0;

    private boolean enable = true;


    /* ************************** PowerJob-worker cluster property ************************** */
    /**
     * Designated PowerJob-worker nodes. Blank value indicates that there is
     * no limit. Non-blank value means to run the corresponding machine(s) only.
     * example: 192.168.1.1:27777,192.168.1.2:27777
     */
    private String designatedWorkers;
    /**
     * Max count of PowerJob-worker nodes.
     */
    private Integer maxWorkerCount = 0;

    /**
     * The id list of the users that need to be notified.
     */
    private List<Long> notifyUserIds;

    private String extra;

    private DispatchStrategy dispatchStrategy;

    private LifeCycle lifeCycle;
    /**
     * alarm config
     */
    private AlarmConfig alarmConfig;

    /**
     * 任务归类，开放给接入方自由定制
     */
    private String tag;

    /**
     * 日志配置，包括日志级别、日志方式等配置信息
     */
    private LogConfig logConfig;


    /**
     * Check non-null properties.
     */
    public void valid() {
        CommonUtils.requireNonNull(jobName, "jobName can't be empty");
        CommonUtils.requireNonNull(appId, "appId can't be empty");
        CommonUtils.requireNonNull(processorInfo, "processorInfo can't be empty");
        CommonUtils.requireNonNull(executeType, "executeType can't be empty");
        CommonUtils.requireNonNull(processorType, "processorType can't be empty");
        CommonUtils.requireNonNull(timeExpressionType, "timeExpressionType can't be empty");
    }

    public DispatchStrategy getDispatchStrategy() {
        if (dispatchStrategy == null) {
            return DispatchStrategy.HEALTH_FIRST;
        }
        return dispatchStrategy;
    }
}
