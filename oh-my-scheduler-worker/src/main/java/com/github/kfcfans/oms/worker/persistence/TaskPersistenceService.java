package com.github.kfcfans.oms.worker.persistence;


import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 任务持久化服务
 *
 * @author tjq
 * @since 2020/3/17
 */
public class TaskPersistenceService {

    private TaskDAO taskDAO = new TaskDAOImpl();
    private static final int MAX_BATCH_SIZE = 50;

    public boolean save(TaskDO task) {
        return taskDAO.save(task);
    }

    public boolean batchSave(List<TaskDO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        if (tasks.size() <= MAX_BATCH_SIZE) {
            return taskDAO.batchSave(tasks);
        }
        List<List<TaskDO>> partition = Lists.partition(tasks, MAX_BATCH_SIZE);
        for (List<TaskDO> p : partition) {
            boolean b = taskDAO.batchSave(p);
            if (!b) {
                return false;
            }
        }
        return true;
    }


    /**
     * 获取 TaskTracker 准备派发给 Worker 执行的 task
     */
    public List<TaskDO> getNeedDispatchTask(String instanceId) {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        query.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
        query.setLimit(100);
        return taskDAO.simpleQuery(query);
    }
}
