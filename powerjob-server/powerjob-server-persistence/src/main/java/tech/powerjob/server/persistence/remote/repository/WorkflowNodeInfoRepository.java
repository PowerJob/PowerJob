package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;


/**
 * WorkflowNodeInfo 数据访问层
 *
 * @author Echo009
 * @since 2021/2/1
 */
public interface WorkflowNodeInfoRepository extends JpaRepository<WorkflowNodeInfoDO, Long> {

    /**
     * 根据工作流id查找所有的节点
     *
     * @param workflowId 工作流id
     * @return 节点信息集合
     */
    List<WorkflowNodeInfoDO> findByWorkflowId(Long workflowId);

    /**
     * 根据工作流节点 ID 删除节点
     *
     * @param workflowId 工作流ID
     * @param id         节点 ID
     * @return 删除记录数
     */
    int deleteByWorkflowIdAndIdNotIn(Long workflowId, List<Long> id);

    /**
     * 删除工作流 ID 为空，且创建时间早于指定时间的节点信息
     *
     * @param crtTimeThreshold 创建时间阈值
     * @return 删除记录条数
     */
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query(value = "delete from WorkflowNodeInfoDO where workflowId is null and gmtCreate < ?1")
    int deleteAllByWorkflowIdIsNullAndGmtCreateBefore(Date crtTimeThreshold);

}
