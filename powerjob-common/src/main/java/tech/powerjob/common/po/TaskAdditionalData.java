package tech.powerjob.common.po;

import lombok.Data;
import lombok.experimental.Accessors;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;

/**
 * @author Echo009
 * @since 2021/9/27
 */
@Data
@Accessors(chain = true)
@NetEaseCustomFeature(CustomFeatureEnum.TASK_ADDITIONAL_DATA)
public class TaskAdditionalData {

    /**
     * 原始触发时间
     * 即当前实例首次扫描出来的期望调度时间，该时间不会因 重试、故障转移 而改变
     */
    private Long originTriggerTime;



}
