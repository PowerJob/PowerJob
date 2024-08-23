package tech.powerjob.common.request.common;

import lombok.Data;

/**
 * 运行任务
 *
 * @author tjq
 * @since 2024/8/23
 */
@Data
public class RunJobRequest {

    /**
     * 目标任务ID
     */
    private Long jobId;

    /**
     * 任务实例参数
     */
    private String instanceParams;

    /**
     * 延迟执行时间
     */
    private Long delay;

    /**
     * 指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）
     */
    private String designatedWorkers;
}
