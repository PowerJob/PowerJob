package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JobLog 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface ExecuteLogRepository extends JpaRepository<ExecuteLogDO, Long> {

    long countByJobIdAndStatusIn(long jobId, List<Integer> status);

    @Query(value = "update execute_log set status = ?2, result = ?3 where instance_id = ?1", nativeQuery = true)
    int updateStatusAndLog(long instanceId, int status, String result);

    List<ExecuteLogDO> findByJobIdIn(List<Long> jobIds);
}
