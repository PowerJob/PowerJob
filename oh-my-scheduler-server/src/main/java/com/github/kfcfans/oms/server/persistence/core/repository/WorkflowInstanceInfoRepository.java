package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInstanceInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<WorkflowInstanceInfoDO> findByWfInstanceId(Long wfInstanceId);

    // 删除历史数据，JPA自带的删除居然是根据ID循环删，2000条数据删了几秒，也太拉垮了吧...
    // 结果只能用 int 接收
    @Modifying
    @Transactional
    @Query(value = "delete from workflow_instance_info where gmt_modified < ?1", nativeQuery = true)
    int deleteAllByGmtModifiedBefore(Date time);

    int countByWorkflowIdAndStatusIn(Long workflowId, List<Integer> status);

    // list 三兄弟
    Page<WorkflowInstanceInfoDO> findByAppId(Long appId, Pageable pageable);
    Page<WorkflowInstanceInfoDO> findByAppIdAndWfInstanceId(Long appId, Long wfInstanceId, Pageable pageable);
    Page<WorkflowInstanceInfoDO> findByAppIdAndWorkflowId(Long appId, Long workflowId, Pageable pageable);
}
