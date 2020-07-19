package com.github.kfcfans.powerjob.server.persistence.core.repository;

import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JobInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface JobInfoRepository extends JpaRepository<JobInfoDO, Long> {


    // 调度专用
    List<JobInfoDO> findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(List<Long> appIds, int status, int timeExpressionType, long time);

    @Query(value = "select id from JobInfoDO where appId in ?1 and status = ?2 and timeExpressionType in ?3")
    List<Long> findByAppIdInAndStatusAndTimeExpressionTypeIn(List<Long> appIds, int status, List<Integer> timeTypes);

    Page<JobInfoDO> findByAppIdAndStatusNot(Long appId, int status, Pageable pageable);

    Page<JobInfoDO> findByAppIdAndJobNameLikeAndStatusNot(Long appId, String condition, int status, Pageable pageable);

    // 校验工作流包含的任务
    long countByAppIdAndStatusAndIdIn(Long appId, int status, List<Long> jobIds);

    long countByAppIdAndStatusNot(long appId, int status);

}
