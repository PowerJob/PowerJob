package com.github.kfcfans.powerjob.worker.core.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kfcfans.powerjob.common.WorkflowContextConstant;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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
     * 用户自定义上下文，通过 {@link OhMyConfig} 初始化
     */
    private Object userContext;

    /**
     * 追加的上下文数据
     */
    private final Map<String,String> appendedContextData = Maps.newConcurrentMap();



    /**
     * 获取工作流上下文 (MAP)，本质上是将 instanceParams 解析成 MAP
     * 初始参数的 key 为 {@link WorkflowContextConstant#CONTEXT_INIT_PARAMS_KEY}
     * 注意，在没有传递初始参数时，通过 CONTEXT_INIT_PARAMS_KEY 获取到的是 null
     *
     * @return 工作流上下文
     * @author Echo009
     * @since 2021/02/04
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    public Map<String, String> fetchWorkflowContext() {
        Map<String, String> res = Maps.newHashMap();
        try {
            Map originMap = JsonUtils.parseObject(instanceParams, Map.class);
            originMap.forEach((k, v) -> res.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            return res;
        } catch (Exception ignore) {
            // ignore
        }
        return Maps.newHashMap();
    }


    /**
     * 往工作流上下文添加数据
     * 注意：如果 key 在当前上下文中已存在，那么会直接覆盖
     */
    public synchronized void appendData2WfContext(String key,Object value){
        String finalValue;
        try {
            // 先判断当前上下文大小是否超出限制
            final int sizeThreshold = OhMyWorker.getConfig().getMaxAppendedWfContextSize();
            if (appendedContextData.size() >= sizeThreshold) {
                log.warn("[TaskContext-{}|{}|{}] appended workflow context data size must be lesser than {}, current appended workflow context data(key={}) will be ignored!",instanceId,taskId,taskName,sizeThreshold,key);
            }
            finalValue = JsonUtils.toJSONStringUnsafe(value);
            final int lengthThreshold = OhMyWorker.getConfig().getMaxAppendedWfContextLength();
            // 判断 key & value 是否超长度限制
            if (key.length() > lengthThreshold || finalValue.length() > lengthThreshold) {
                log.warn("[TaskContext-{}|{}|{}] appended workflow context data length must be shorter than {}, current appended workflow context data(key={}) will be ignored!",instanceId,taskId,taskName,lengthThreshold,key);
                return;
            }
        } catch (Exception e) {
            log.warn("[TaskContext-{}|{}|{}] fail to append data to workflow context, key : {}",instanceId,taskId,taskName, key);
            return;
        }
        appendedContextData.put(key, JsonUtils.toJSONString(value));
    }

}
