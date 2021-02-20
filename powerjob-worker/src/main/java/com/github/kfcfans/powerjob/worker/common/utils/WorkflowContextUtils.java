package com.github.kfcfans.powerjob.worker.common.utils;

import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;

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


    public static boolean isExceededLengthLimit(Map<String, String> appendedWfContext) {

        String jsonString = JsonUtils.toJSONString(appendedWfContext);
        if (jsonString == null) {
            // impossible
            return true;
        }
        int maxAppendedWfContextLength = OhMyWorker.getConfig().getMaxAppendedWfContextLength();

        return maxAppendedWfContextLength < jsonString.length();

    }

}
