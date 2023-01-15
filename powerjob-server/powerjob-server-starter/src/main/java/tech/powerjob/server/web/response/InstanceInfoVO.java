package tech.powerjob.server.web.response;

import tech.powerjob.common.OmsConstant;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import lombok.Data;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;

/**
 * InstanceInfo 对外展示对象
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
public class InstanceInfoVO {

    /**
     * 任务ID（JS精度丢失）
     */
    private String jobId;
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务实例ID（JS精度丢失）
     */
    private String instanceId;
    /**
     * 该任务实例所属的 workflow ID，仅 workflow 任务存在
     */
    private String wfInstanceId;
    /**
     *  执行结果
     */
    private String result;
    /**
     * TaskTracker地址
     */
    private String taskTrackerAddress;
    /**
     * 总共执行的次数（用于重试判断）
     */
    private Long runningTimes;
    private int status;

    /* ********** 不一致区域 ********** */
    /**
     * 实际触发时间（需要格式化为人看得懂的时间）
     */
    private String actualTriggerTime;
    /**
     * 结束时间（同理，需要格式化）
     */
    private String finishedTime;

    public static InstanceInfoVO from(InstanceInfoDO instanceInfoDo, String jobName) {
        InstanceInfoVO instanceInfoVO = new InstanceInfoVO();
        BeanUtils.copyProperties(instanceInfoDo, instanceInfoVO);

        // 额外设置任务名称，提高可读性
        instanceInfoVO.setJobName(jobName);

        // ID 转化为 String（JS精度丢失）
        instanceInfoVO.setJobId(instanceInfoDo.getJobId().toString());
        instanceInfoVO.setInstanceId(instanceInfoDo.getInstanceId().toString());
        if (instanceInfoDo.getWfInstanceId() == null) {
            instanceInfoVO.setWfInstanceId(OmsConstant.NONE);
        }else {
            instanceInfoVO.setWfInstanceId(String.valueOf(instanceInfoDo.getWfInstanceId()));
        }

        // 格式化时间
        if (instanceInfoDo.getActualTriggerTime() == null) {
            instanceInfoVO.setActualTriggerTime(OmsConstant.NONE);
        }else {
            instanceInfoVO.setActualTriggerTime(DateFormatUtils.format(instanceInfoDo.getActualTriggerTime(), OmsConstant.TIME_PATTERN));
        }
        if (instanceInfoDo.getFinishedTime() == null) {
            instanceInfoVO.setFinishedTime(OmsConstant.NONE);
        }else {
            instanceInfoVO.setFinishedTime(DateFormatUtils.format(instanceInfoDo.getFinishedTime(), OmsConstant.TIME_PATTERN));
        }

        return instanceInfoVO;
    }
}
