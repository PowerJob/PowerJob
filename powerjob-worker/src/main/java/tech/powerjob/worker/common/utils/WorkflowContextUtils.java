package tech.powerjob.worker.common.utils;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CollectionUtils;

import java.util.Map;

/**
 * 工作流上下文工具类
 *
 * @author Echo009
 * @since 2021/2/20
 */
public class WorkflowContextUtils {

    public static boolean isExceededLengthLimit(Map<String, String> appendedWfContext, int maxLength) {
        if (CollectionUtils.isEmpty(appendedWfContext)) {
            return false;
        }
        String jsonString = JsonUtils.toJSONString(appendedWfContext);
        return maxLength < jsonString.length();
    }

}
