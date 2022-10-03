package tech.powerjob.server.web.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.AlarmConfig;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.model.LifeCycle;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.SJ;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;

/**
 * JobInfo 对外展示对象
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
public class JobInfoVO {

    private Long id;

    /* ************************** 任务基本信息 ************************** */
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务描述
     */
    private String jobDescription;
    /**
     * 任务所属的应用ID
     */
    private Long appId;
    /**
     * 任务自带的参数
     */
    private String jobParams;

    /* ************************** 定时参数 ************************** */
    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private String timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    /* ************************** 执行方式 ************************** */
    /**
     * 执行类型，单机/广播/MR
     */
    private String executeType;
    /**
     * 执行器类型，Java/Shell
     */
    private String processorType;
    /**
     * 执行器信息
     */
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    /**
     * 最大同时运行任务数，默认 1
     */
    private Integer maxInstanceNum;
    /**
     * 并发度，同时执行某个任务的最大线程数量
     */
    private Integer concurrency;
    /**
     * 任务整体超时时间
     */
    private Long instanceTimeLimit;

    /* ************************** 重试配置 ************************** */

    private Integer instanceRetryNum;
    private Integer taskRetryNum;

    /**
     * 1 正常运行，2 停止（不再调度）
     */
    private boolean enable;
    /**
     * 下一次调度时间
     */
    private Long nextTriggerTime;
    /**
     * 下一次调度时间（文字版）
     */
    private String nextTriggerTimeStr;

    /* ************************** 繁忙机器配置 ************************** */
    /**
     * 最低CPU核心数量，0代表不限
     */
    private double minCpuCores;
    /**
     * 最低内存空间，单位 GB，0代表不限
     */
    private double minMemorySpace;
    /**
     * 最低磁盘空间，单位 GB，0代表不限
     */
    private double minDiskSpace;

    private Date gmtCreate;
    private Date gmtModified;

    /* ************************** 集群配置 ************************** */
    /**
     * 指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）
     */
    private String designatedWorkers;
    /**
     * 最大机器数量
     */
    private Integer maxWorkerCount;

    /**
     * 报警用户ID列表
     */
    private List<String> notifyUserIds;

    private String extra;

    private String dispatchStrategy;

    private LifeCycle lifeCycle;

    private AlarmConfig alarmConfig;

    /**
     * 任务归类，开放给接入方自由定制
     */
    private String tag;

    /**
     * 日志配置，包括日志级别、日志方式等配置信息
     */
    private LogConfig logConfig;

    public static JobInfoVO from(JobInfoDO jobInfoDO) {
        JobInfoVO jobInfoVO = new JobInfoVO();
        BeanUtils.copyProperties(jobInfoDO, jobInfoVO);

        TimeExpressionType timeExpressionType = TimeExpressionType.of(jobInfoDO.getTimeExpressionType());
        ExecuteType executeType = ExecuteType.of(jobInfoDO.getExecuteType());
        ProcessorType processorType = ProcessorType.of(jobInfoDO.getProcessorType());
        DispatchStrategy dispatchStrategy = DispatchStrategy.of(jobInfoDO.getDispatchStrategy());

        jobInfoVO.setTimeExpressionType(timeExpressionType.name());
        jobInfoVO.setExecuteType(executeType.name());
        jobInfoVO.setProcessorType(processorType.name());
        jobInfoVO.setEnable(jobInfoDO.getStatus() == SwitchableStatus.ENABLE.getV());
        jobInfoVO.setDispatchStrategy(dispatchStrategy.name());

        if (!StringUtils.isEmpty(jobInfoDO.getNotifyUserIds())) {
            jobInfoVO.setNotifyUserIds(SJ.COMMA_SPLITTER.splitToList(jobInfoDO.getNotifyUserIds()));
        }else {
            jobInfoVO.setNotifyUserIds(Lists.newLinkedList());
        }
        jobInfoVO.setNextTriggerTimeStr(CommonUtils.formatTime(jobInfoDO.getNextTriggerTime()));

        if (!StringUtils.isEmpty(jobInfoDO.getAlarmConfig())){
            jobInfoVO.setAlarmConfig(JSON.parseObject(jobInfoDO.getAlarmConfig(),AlarmConfig.class));
        } else {
            jobInfoVO.setAlarmConfig(new AlarmConfig());
        }
        if (!StringUtils.isEmpty(jobInfoDO.getLifecycle())){
            jobInfoVO.setLifeCycle(LifeCycle.parse(jobInfoDO.getLifecycle()));
        }

        if (!StringUtils.isEmpty(jobInfoDO.getLogConfig())) {
            jobInfoVO.setLogConfig(JSONObject.parseObject(jobInfoDO.getLogConfig(), LogConfig.class));
        } else {
            // 不存在 job 配置时防止前端报错
            jobInfoVO.setLogConfig(new LogConfig());
        }

        return jobInfoVO;
    }
}
