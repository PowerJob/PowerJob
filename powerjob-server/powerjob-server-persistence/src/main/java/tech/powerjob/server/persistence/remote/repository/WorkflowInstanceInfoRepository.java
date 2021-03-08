package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 工作流运行实例数据操作
 *
 * @author tjq
 * @since 2020/5/26
 */
public interface WorkflowInstanceInfoRepository extends JpaRepository<WorkflowInstanceInfoDO, Long> {

    /**
     * 查找对应工作流实例
     * @param wfInstanceId 实例 ID
     * @return 工作流实例
     */
    Optional<WorkflowInstanceInfoDO> findByWfInstanceId(Long wfInstanceId);

    /**
     * 删除历史数据，JPA自带的删除居然是根据ID循环删，2000条数据删了几秒，也太拉垮了吧...
     * 结果只能用 int 接收
     * @param time 更新时间阈值
     * @param status 状态列表
     * @return 删除的记录条数
     */
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query(value = "delete from WorkflowInstanceInfoDO where gmtModified < ?1 and status in ?2")
    int deleteAllByGmtModifiedBeforeAndStatusIn(Date time, List<Integer> status);

    /**
     * 统计该工作流下处于对应状态的实例数量
     * @param workflowId 工作流 ID
     * @param status 状态列表
     * @return 更新的记录条数
     */
    int countByWorkflowIdAndStatusIn(Long workflowId, List<Integer> status);

    /**
     * 加载期望调度时间小于给定阈值的
     * @param appIds 应用 ID 列表
     * @param status 状态
     * @param time 期望调度时间阈值
     * @return 工作流实例列表
     */
    List<WorkflowInstanceInfoDO> findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(List<Long> appIds, int status, long time);
}
