package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.JobLogDO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JobLog 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface JobLogRepository extends JpaRepository<JobLogDO, Long> {

    long countByJobIdAndStatus(Long jobId, Integer status);

}
