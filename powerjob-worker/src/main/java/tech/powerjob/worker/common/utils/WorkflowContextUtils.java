package tech.powerjob.worker.common.utils;

import tech.powerjob.common.serialize.JsonUtils;

import java.util.Map;

/**
 * 工作流上下文工具类
 *
 * @author Echo009
 * @since 2021/2/20
 */
public class WorkflowContextUtils {

    private WorkflowContextUtils() {

    }


    public static boolean isExceededLengthLimit(Map<String, String> appendedWfContext, int maxLength) {

        String jsonString = JsonUtils.toJSONString(appendedWfContext);
        if (jsonString == null) {
            // impossible
            return true;
        }

        return maxLength < jsonString.length();

    }

}
