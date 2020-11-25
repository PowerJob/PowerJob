package com.github.kfcfans.powerjob.server.persistence.core.repository;

import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
public interface InstanceInfoRepository extends JpaRepository<InstanceInfoDO, Long> {

    /**
     * 统计当前JOB有多少实例正在运行
     */
    long countByJobIdAndStatusIn(long jobId, List<Integer> status);

    List<InstanceInfoDO> findByJobIdAndStatusIn(long jobId, List<Integer> status);


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
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = ?2, runningTimes = ?3, actualTriggerTime = ?4, finishedTime = ?5, taskTrackerAddress = ?6, result = ?7, instanceParams = ?8, gmtModified = ?9 where instanceId = ?1")
    int update4TriggerFailed(long instanceId, int status, long runningTimes, long actualTriggerTime, long finishedTime, String taskTrackerAddress, String result, String instanceParams, Date modifyTime);

    @Transactional
    @Modifying
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = ?2, runningTimes = ?3, actualTriggerTime = ?4, taskTrackerAddress = ?5, instanceParams = ?6, gmtModified = ?7 where instanceId = ?1")
    int update4TriggerSucceed(long instanceId, int status, long runningTimes, long actualTriggerTime, String taskTrackerAddress, String instanceParams, Date modifyTime);

    @Modifying
    @Transactional
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = ?2, runningTimes = ?3, gmtModified = ?4 where instanceId = ?1")
    int update4FrequentJob(long instanceId, int status, long runningTimes, Date modifyTime);

    // 状态检查三兄弟，对应 WAITING_DISPATCH 、 WAITING_WORKER_RECEIVE 和 RUNNING 三阶段
    // 数据量一般不大，就不单独写SQL优化 IO 了
    List<InstanceInfoDO> findByAppIdInAndStatusAndExpectedTriggerTimeLessThan(List<Long> jobIds, int status, long time);
    List<InstanceInfoDO> findByAppIdInAndStatusAndActualTriggerTimeLessThan(List<Long> jobIds, int status, long time);
    List<InstanceInfoDO> findByAppIdInAndStatusAndGmtModifiedBefore(List<Long> jobIds, int status, Date time);

    InstanceInfoDO findByInstanceId(long instanceId);

    // 数据统计
    long countByAppIdAndStatus(long appId, int status);
    long countByAppIdAndStatusAndGmtCreateAfter(long appId, int status, Date time);

    @Query(value = "select distinct jobId from InstanceInfoDO where jobId in ?1 and status in ?2")
    List<Long> findByJobIdInAndStatusIn(List<Long> jobIds, List<Integer> status);

    // 删除历史数据，JPA自带的删除居然是根据ID循环删，2000条数据删了几秒，也太拉垮了吧...
    // 结果只能用 int 接收
    @Modifying
    @Transactional
    @Query(value = "delete from InstanceInfoDO where gmtModified < ?1 and status in ?2")
    int deleteAllByGmtModifiedBeforeAndStatusIn(Date time, List<Integer> status);
}
