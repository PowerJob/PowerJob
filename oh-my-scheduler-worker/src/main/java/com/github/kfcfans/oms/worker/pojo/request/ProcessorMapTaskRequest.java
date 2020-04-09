package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.oms.worker.common.ThreadLocalStore;
import com.github.kfcfans.oms.worker.common.utils.SerializerUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
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

    private Long instanceId;
    private Long subInstanceId;

    private String taskName;
    private List<SubTask> subTasks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTask {
        private String taskId;
        private byte[] taskContent;
    }

    public ProcessorMapTaskRequest(TaskDO parentTask, List<?> subTaskList, String taskName) {

        this.instanceId = parentTask.getInstanceId();
        this.subInstanceId = parentTask.getSubInstanceId();
        this.taskName = taskName;
        this.subTasks = Lists.newLinkedList();

        subTaskList.forEach(subTask -> {
            // 同一个 Task 内部可能多次 Map，因此还是要确保线程级别的唯一
            String subTaskId = parentTask.getTaskId() + "." + ThreadLocalStore.getTaskIDAddr().getAndIncrement();
            // 写入类名，方便反序列化
            subTasks.add(new SubTask(subTaskId, SerializerUtils.serialize(subTask)));
        });
    }
}
