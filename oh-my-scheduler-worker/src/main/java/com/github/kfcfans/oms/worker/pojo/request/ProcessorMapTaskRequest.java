package com.github.kfcfans.oms.worker.pojo.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * WorkerMapTaskRequest
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@NoArgsConstructor
public class ProcessorMapTaskRequest implements Serializable {

    private String instanceId;

    private String taskName;
    private List<SubTask> subTasks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTask {
        private String taskId;
        private byte[] taskContent;
    }

    public ProcessorMapTaskRequest(TaskContext taskContext, List<?> subTaskList, String taskName) {

        this.instanceId = taskContext.getInstanceId();
        this.taskName = taskName;
        this.subTasks = Lists.newLinkedList();

        for (int i = 0; i < subTaskList.size(); i++) {
            // 不同执行线程之间，前缀（taskId）不同，该ID可以保证分布式唯一
            String subTaskId = taskContext.getTaskId() + "." + i;
            // 写入类名，方便反序列化
            byte[] content = JSON.toJSONBytes(subTaskList.get(i), SerializerFeature.WriteClassName);
            subTasks.add(new SubTask(subTaskId, content));
        }

    }
}
