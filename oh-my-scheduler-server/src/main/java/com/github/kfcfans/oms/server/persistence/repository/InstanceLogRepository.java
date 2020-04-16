package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

/**
 * JobLog 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface InstanceLogRepository extends JpaRepository<InstanceLogDO, Long> {

    /**
     * 统计当前JOB有多少实例正在运行
     */
    long countByJobIdAndStatusIn(long jobId, List<Integer> status);

    List<InstanceLogDO> findByJobIdAndStatusIn(long jobId, List<Integer> status);


    /**
     * 更新任务执行记录内容（DispatchService专用）
     * @param instanceId 任务实例ID，分布式唯一
     * @param status 任务实例运行状态
     * @param runningTimes 运行次数
     * @param result 结果
     * @return 更新数量
     */
    @Transactional
    @Modifying
    @Query(value = "update instance_log set status = ?2, running_times = ?3, actual_trigger_time = ?4, task_tracker_address = ?5, result = ?6, gmt_modified = now() where instance_id = ?1", nativeQuery = true)
    int update4Trigger(long instanceId, int status, long runningTimes, long actualTriggerTime, String taskTrackerAddress, String result);

    @Modifying
    @Transactional
    @Query(value = "update instance_log set status = ?2, running_times = ?3, gmt_modified = now() where instance_id = ?1", nativeQuery = true)
    int update4FrequentJob(long instanceId, int status, long runningTimes);

    // 状态检查三兄弟，对应 WAITING_DISPATCH 、 WAITING_WORKER_RECEIVE 和 RUNNING 三阶段
    List<InstanceLogDO> findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(List<Long> jobIds, int status, long time);
    List<InstanceLogDO> findByAppIdInAndStatusAndActualTriggerTimeLessThan(List<Long> jobIds, int status, long time);
    List<InstanceLogDO> findByAppIdInAndStatusAndGmtModifiedBefore(List<Long> jobIds, int status, Date time);

    InstanceLogDO findByInstanceId(long instanceId);

    Page<InstanceLogDO> findByAppId(long appId, Pageable pageable);
    Page<InstanceLogDO> findByJobId(long jobId, Pageable pageable);
    // 只会有一条数据，只是为了统一
    Page<InstanceLogDO> findByInstanceId(long instanceId, Pageable pageable);

    // 数据统计
    long countByAppIdAndStatus(long appId, int status);
    long countByAppIdAndStatusAndGmtCreateAfter(long appId, int status, Date time);
}
