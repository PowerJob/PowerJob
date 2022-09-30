package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * JobInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface JobInfoRepository extends JpaRepository<JobInfoDO, Long>, JpaSpecificationExecutor<JobInfoDO> {


    /**
     * 调度专用
     */
    List<JobInfoDO> findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(List<Long> appIds, int status, int timeExpressionType, long time);

    @Query(value = "select id from JobInfoDO where appId in ?1 and status = ?2 and timeExpressionType in ?3")
    List<Long> findByAppIdInAndStatusAndTimeExpressionTypeIn(List<Long> appIds, int status, List<Integer> timeTypes);

    Page<JobInfoDO> findByAppIdAndStatusNot(Long appId, int status, Pageable pageable);

    Page<JobInfoDO> findByAppIdAndJobNameLikeAndStatusNot(Long appId, String condition, int status, Pageable pageable);

    /**
     * 校验工作流包含的任务
     * @param appId APP ID
     * @param statusSet 状态列表
     * @param jobIds 任务ID
     * @return 数量
     */
    long countByAppIdAndStatusInAndIdIn(Long appId, Set<Integer> statusSet , Set<Long> jobIds);

    long countByAppIdAndStatusNot(long appId, int status);

    List<JobInfoDO> findByAppId(Long appId);

    List<JobInfoDO> findByIdIn(Collection<Long> jobIds);

}
