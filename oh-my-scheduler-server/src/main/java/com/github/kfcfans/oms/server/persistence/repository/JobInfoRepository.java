package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
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

    @Query(value = "select id from job_info where app_id in ?1 and status = ?2 and time_expression_type in ?3", nativeQuery = true)
    List<Long> findByAppIdInAndStatusAndTimeExpressionTypeIn(List<Long> appIds, int status, List<Integer> timeTypes);

    Page<JobInfoDO> findByAppIdAndStatusNot(Long appId, Pageable pageable, int status);

    Page<JobInfoDO> findByAppIdAndJobNameLikeAndStatusNot(Long appId, String condition, int status, Pageable pageable);


    long countByAppId(long appId);

}
