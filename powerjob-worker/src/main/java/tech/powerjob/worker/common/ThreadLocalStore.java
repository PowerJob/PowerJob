package tech.powerjob.worker.common;

import tech.powerjob.worker.persistence.TaskDO;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储一些不方便直接传递的东西
 * #attention：警惕内存泄漏问题，执行完毕后手动释放
 *
 * @author tjq
 * @since 2020/3/18
 */
public class ThreadLocalStore {

    private static final ThreadLocal<TaskDO> TASK_THREAD_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<WorkerRuntime> RUNTIME_META_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<AtomicLong> TASK_ID_THREAD_LOCAL = new ThreadLocal<>();


    public static TaskDO getTask() {
        return TASK_THREAD_LOCAL.get();
    }

    public static void setTask(TaskDO task) {
        TASK_THREAD_LOCAL.set(task);
    }

    public static WorkerRuntime getRuntimeMeta() {
        return RUNTIME_META_LOCAL.get();
    }

    public static void setRuntimeMeta(WorkerRuntime workerRuntime) {
        RUNTIME_META_LOCAL.set(workerRuntime);
    }

    public static AtomicLong getTaskIDAddr() {
        if (TASK_ID_THREAD_LOCAL.get() == null) {
            TASK_ID_THREAD_LOCAL.set(new AtomicLong(0));
        }
        return TASK_ID_THREAD_LOCAL.get();
    }

    public static void clear() {
        TASK_ID_THREAD_LOCAL.remove();
        RUNTIME_META_LOCAL.remove();
        TASK_THREAD_LOCAL.remove();
    }

}
