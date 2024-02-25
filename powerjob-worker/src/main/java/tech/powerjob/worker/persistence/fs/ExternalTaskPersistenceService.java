package tech.powerjob.worker.persistence.fs;


import tech.powerjob.worker.persistence.TaskDO;

import java.io.Closeable;
import java.util.List;

/**
 * 外部任务持久化服务
 *
 * @author tjq
 * @since 2024/2/22
 */
public interface ExternalTaskPersistenceService extends Closeable {

    boolean persistPendingTask(List<TaskDO> tasks);

    List<TaskDO> readPendingTask();

    boolean persistFinishedTask(List<TaskDO> tasks);

    List<TaskDO> readFinishedTask();
}
