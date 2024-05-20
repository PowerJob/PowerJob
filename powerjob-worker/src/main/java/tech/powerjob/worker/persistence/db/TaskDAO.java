package tech.powerjob.worker.persistence.db;

import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.persistence.TaskDO;

import java.sql.SQLException;
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
    boolean save(TaskDO task) throws SQLException;
    boolean batchSave(Collection<TaskDO> tasks) throws SQLException;

    boolean simpleDelete(SimpleTaskQuery condition) throws SQLException;

    List<TaskDO> simpleQuery(SimpleTaskQuery query) throws SQLException;

    List<Map<String, Object>> simpleQueryPlus(SimpleTaskQuery query) throws SQLException;

    boolean simpleUpdate(SimpleTaskQuery condition, TaskDO updateField) throws SQLException;

    /**
     * 查询所有子任务的执行结果 (为了性能特殊定制，主要是内存占用，如果使用 simpleQueryPlus，内存中需要同时存在3份数据 ？是同时存在3份数据吗)
     */
    List<TaskResult> getAllTaskResult(Long instanceId, Long subInstanceId) throws SQLException;

    /**
     * 更新任务状态（result可能出现千奇百怪的字符，比如 ' ，只能特殊定制SQL直接写入）
     */
    boolean updateTaskStatus(Long instanceId, String taskId, int status, long lastReportTime, String result) throws SQLException;

}
