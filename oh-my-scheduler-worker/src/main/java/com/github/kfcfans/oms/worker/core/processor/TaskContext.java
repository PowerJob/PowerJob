package com.github.kfcfans.oms.worker.core.processor;

import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.worker.log.OmsLogger;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 任务上下文
 * 概念统一，所有的worker只处理Task，Job和JobInstance的概念只存在于Server和TaskTracker
 * 单机任务：整个Job变成一个Task
 * 广播任务：整个job变成一堆一样的Task
 * MR 任务：被map出来的任务都视为根Task的子Task
 *
 * @author tjq
 * @since 2020/3/18
 */
@Getter
@Setter
@ToString
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
     * 若该任务为工作流的某个节点，则该值为上游任务传递下来的数据，推荐通过 {@link TaskContext#fetchUpstreamTaskResult()} 方法获取
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
    private OmsLogger omsLogger;
    /**
     * 用户自定义上下文
     */
    private Object userContext;


    /**
     * 获取工作流上游任务传递的数据（仅该任务实例由工作流触发时存在）
     * @return key: 上游任务的 jobId；value: 上游任务的 ProcessResult#result
     */
    @SuppressWarnings("rawtypes, unchecked")
    public Map<Long, String> fetchUpstreamTaskResult() {
        Map<Long, String> res = Maps.newHashMap();
        if (StringUtils.isEmpty(instanceParams)) {
            return res;
        }
        try {
            Map originMap = JsonUtils.parseObject(instanceParams, Map.class);
            originMap.forEach((k, v) -> res.put(Long.valueOf(String.valueOf(k)), String.valueOf(v)));
            return res;
        }catch (Exception ignore) {
        }
        return Maps.newHashMap();
    }
}
