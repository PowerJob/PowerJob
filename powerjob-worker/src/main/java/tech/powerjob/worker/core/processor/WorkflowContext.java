package tech.powerjob.worker.core.processor;

import tech.powerjob.common.WorkflowContextConstant;
import tech.powerjob.common.serialize.JsonUtils;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 工作流上下文
 *
 * @author Echo009
 * @since 2021/2/19
 */
@Getter
@Slf4j
public class WorkflowContext {
    /**
     * 工作流实例 ID
     */
    private final Long  wfInstanceId;
    /**
     * 当前工作流上下文数据
     * 这里的 data 实际上等价于 {@link TaskContext} 中的 instanceParams
     */
    private final Map<String, String> data = Maps.newHashMap();
    /**
     * 追加的上下文信息
     */
    private final Map<String, String> appendedContextData = Maps.newConcurrentMap();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public WorkflowContext(Long wfInstanceId, String data) {
        this.wfInstanceId = wfInstanceId;
        if (wfInstanceId == null || StringUtils.isBlank(data)) {
            return;
        }
        try {
            Map originMap = JsonUtils.parseObject(data, Map.class);
            originMap.forEach((k, v) -> this.data.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        } catch (Exception exception) {
            log.warn("[WorkflowContext-{}] parse workflow context failed, {}", wfInstanceId, exception.getMessage());
        }
    }

    /**
     * 获取工作流上下文 (MAP)，本质上是将 data 解析成 MAP
     * 初始参数的 key 为 {@link WorkflowContextConstant#CONTEXT_INIT_PARAMS_KEY}
     * 注意，在没有传递初始参数时，通过 CONTEXT_INIT_PARAMS_KEY 获取到的是 null
     *
     * @return 工作流上下文
     * @author Echo009
     * @since 2021/02/04
     */
    public Map<String, String> fetchWorkflowContext() {
        return data;
    }

    /**
     * 往工作流上下文添加数据
     * 注意：如果 key 在当前上下文中已存在，那么会直接覆盖
     */
    public void appendData2WfContext(String key, Object value) {
        if (wfInstanceId == null) {
            // 非工作流中的任务，直接忽略
            return;
        }
        String finalValue;
        try {
            // 这里不限制长度，完成任务之后上报至 TaskTracker 时再校验
            finalValue = JsonUtils.toJSONStringUnsafe(value);
        } catch (Exception e) {
            log.warn("[WorkflowContext-{}] fail to append data to workflow context, key : {}", wfInstanceId, key);
            return;
        }
        appendedContextData.put(key, finalValue);
    }


}
