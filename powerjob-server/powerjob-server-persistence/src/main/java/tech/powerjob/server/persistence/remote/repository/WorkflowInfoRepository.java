package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * DAG 工作流 数据操作层
 *
 * @author tjq
 * @since 2020/5/26
 */
public interface WorkflowInfoRepository extends JpaRepository<WorkflowInfoDO, Long> {


    List<WorkflowInfoDO> findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(List<Long> appIds, int status, int timeExpressionType, long time);

    /**
     * 查询指定 APP 下所有的工作流信息
     *
     * @param appId APP ID
     * @return 该 APP 下的所有工作流信息
     */
    List<WorkflowInfoDO> findByAppId(Long appId);

    /**
     * 对外查询（list）三兄弟
     */
    Page<WorkflowInfoDO> findByAppIdAndStatusNot(Long appId, int nStatus, Pageable pageable);

    Page<WorkflowInfoDO> findByIdAndStatusNot(Long id, int nStatus, Pageable pageable);

    Page<WorkflowInfoDO> findByAppIdAndStatusNotAndWfNameLike(Long appId, int nStatus, String condition, Pageable pageable);
}
