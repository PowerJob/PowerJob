package com.github.kfcfans.oms.worker.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 任务持久化接口
 *
 * @author tjq
 * @since 2020/3/17
 */
public interface TaskDAO {

    /**
     * 初始化任务表
     */
    void initTable() throws Exception;

    /**
     * 插入任务数据
     */
    boolean save(TaskDO task);
    boolean batchSave(Collection<TaskDO> tasks);

    /**
     * 更新任务数据，必须有主键 instanceId + taskId
     */
    boolean update(TaskDO task);

    int batchDelete(String instanceId, List<String> taskIds);

    List<TaskDO> simpleQuery(SimpleTaskQuery query);

    List<Map<String, Object>> simpleQueryPlus(SimpleTaskQuery query);

    boolean simpleUpdate(SimpleTaskQuery condition, TaskDO updateField);

    /**
     * 查询 taskId -> taskResult (为了性能特殊定制，主要是内存占用，如果使用 simpleQueryPlus，内存中需要同时存在3份数据 ？是同时存在3份数据吗)
     */
    Map<String, String> queryTaskId2TaskResult(String instanceId);

}
