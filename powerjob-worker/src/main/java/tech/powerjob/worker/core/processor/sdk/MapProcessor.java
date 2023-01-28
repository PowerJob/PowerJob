package tech.powerjob.worker.core.processor.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.powerjob.common.exception.PowerJobCheckedException;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.worker.common.ThreadLocalStore;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.utils.TransportUtils;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.request.ProcessorMapTaskRequest;

import java.util.List;

/**
 * Map 处理器，允许开发者自定义拆分任务进行分布式执行
 *
 * @author tjq
 * @since 2020/4/17
 */
public interface MapProcessor extends BasicProcessor {

    Logger log = LoggerFactory.getLogger(MapProcessor.class);

    int RECOMMEND_BATCH_SIZE = 200;

    /**
     * 分发子任务
     * @param taskList 子任务，再次执行时可通过 TaskContext#getSubTask 获取
     * @param taskName 子任务名称，即子任务处理器中 TaskContext#getTaskName 获取到的值
     * @throws PowerJobCheckedException map 失败将抛出异常
     */
    default void map(List<?> taskList, String taskName) throws PowerJobCheckedException {

        if (CollectionUtils.isEmpty(taskList)) {
            return;
        }

        TaskDO task = ThreadLocalStore.getTask();
        WorkerRuntime workerRuntime = ThreadLocalStore.getRuntimeMeta();

        if (taskList.size() > RECOMMEND_BATCH_SIZE) {
            log.warn("[Map-{}] map task size is too large, network maybe overload... please try to split the tasks.", task.getInstanceId());
        }

        // 修复 map 任务命名和根任务名或者最终任务名称一致导致的问题（无限生成子任务或者直接失败）
        if (TaskConstant.ROOT_TASK_NAME.equals(taskName) || TaskConstant.LAST_TASK_NAME.equals(taskName)) {
            log.warn("[Map-{}] illegal map task name : {}! please do not use 'OMS_ROOT_TASK' or 'OMS_LAST_TASK' as map task name. as a precaution, it will be renamed 'X-{}' automatically." ,task.getInstanceId() ,taskName , taskName);
            taskName ="X-"+taskName;
        }

        // 1. 构造请求
        ProcessorMapTaskRequest req = new ProcessorMapTaskRequest(task, taskList, taskName);

        // 2. 可靠发送请求（任务不允许丢失，需要使用 ask 方法，失败抛异常）
        boolean requestSucceed = TransportUtils.reliableMapTask(req, task.getAddress(), workerRuntime);

        if (requestSucceed) {
            log.info("[Map-{}] map task[name={},num={}] successfully!", task.getInstanceId(), taskName, taskList.size());
        }else {
            throw new PowerJobCheckedException("map failed for task: " + taskName);
        }
    }

    /**
     * 是否为根任务
     * @return true -> 根任务 / false -> 非根任务
     */
    default boolean isRootTask() {
        TaskDO task = ThreadLocalStore.getTask();
        return TaskConstant.ROOT_TASK_NAME.equals(task.getTaskName());
    }
}
