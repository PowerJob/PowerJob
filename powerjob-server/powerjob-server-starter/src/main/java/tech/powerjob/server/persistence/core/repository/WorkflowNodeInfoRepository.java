package tech.powerjob.server.persistence.core.repository;

import tech.powerjob.server.persistence.core.model.WorkflowNodeInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * @param id 节点 ID
     * @return 删除记录数
     */
    int deleteByWorkflowIdAndIdNotIn(Long workflowId,List<Long> id);

}
