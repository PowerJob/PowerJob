package tech.powerjob.worker.core.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.log.OmsLogger;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务上下文
 * 概念统一，所有的worker只处理Task，Job和JobInstance的概念只存在于Server和TaskTracker
 * 单机任务：整个Job变成一个Task
 * 广播任务：整个job变成一堆一样的Task
 * MR 任务：被map出来的任务都视为根Task的子Task
 * <p>
 * 2021/02/04 移除 fetchUpstreamTaskResult 方法
 *
 * @author tjq
 * @author Echo009
 * @since 2020/3/18
 */
@Getter
@Setter
@ToString
@Slf4j
public class TaskContext {

    private Long jobId;

    private Long instanceId;

    private Long subInstanceId;

    private String taskId;

    private String taskName;
    /**
     * 通过控制台传递的参数
     */
    private String jobParams;
    /**
     * 任务实例运行中参数
     * 若该任务实例通过 OpenAPI 触发，则该值为 OpenAPI 传递的参数
     * 若该任务为工作流的某个节点，则该值为工作流实例的上下文 ( wfContext )
     */
    private String instanceParams;
    /**
     * 最大重试次数
     */
    private int maxRetryTimes;
    /**
     * 当前重试次数
     */
    private int currentRetryTimes;
    /**
     * 子任务对象，通过Map/MapReduce处理器的map方法生成
     */
    private Object subTask;
    /**
     * 在线日志记录
     */
    @JsonIgnore
    private OmsLogger omsLogger;
    /**
     * 用户自定义上下文，通过 {@link PowerJobWorkerConfig} 初始化
     */
    private Object userContext;
    /**
     * 工作流上下文数据
     */
    private WorkflowContext workflowContext;

}
