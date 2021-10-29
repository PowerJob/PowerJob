package com.netease.mail.chronos.executor.support.common;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Echo009
 * @since 2021/10/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSplitParam {

    private Integer batchSize;

    private Integer maxSize;


    public static TaskSplitParam parseOrDefault(String json, int batchSize, int maxSize) {
        TaskSplitParam taskSplitParam = null;
        try {
            taskSplitParam = JSON.parseObject(json, TaskSplitParam.class);
        } catch (Exception ignore) {
            //
        }
        if (taskSplitParam == null) {
            taskSplitParam = new TaskSplitParam(50, 10000);
        }
        if (taskSplitParam.getBatchSize() == null || taskSplitParam.getBatchSize() < 0) {
            taskSplitParam.setBatchSize(batchSize);
        }
        if (taskSplitParam.getMaxSize() == null || taskSplitParam.getBatchSize() < 0) {
            taskSplitParam.setMaxSize(maxSize);
        }
        return taskSplitParam;

    }

}
