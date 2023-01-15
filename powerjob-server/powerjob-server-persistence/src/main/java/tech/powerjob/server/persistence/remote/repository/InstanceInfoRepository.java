package tech.powerjob.server.persistence.remote.repository;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

/**
 * JobLog 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface InstanceInfoRepository extends JpaRepository<InstanceInfoDO, Long>, JpaSpecificationExecutor<InstanceInfoDO> {

    /**
     * 统计当前JOB有多少实例正在运行
     */
    @Query(value = "select count(*) from InstanceInfoDO where jobId = ?1 and status in ?2")
    long countByJobIdAndStatusIn(long jobId, List<Integer> status);

    List<InstanceInfoDO> findByJobIdAndStatusIn(long jobId, List<Integer> status);

    /**
     * 更新状态变更信息
     *
     * @param lastReportTime 最近一次上报时间
     * @param modifyTime     更新时间
     * @param runningTimes   运行次数
     * @param instanceId     实例 ID
     * @param status         目标状态
     * @param oldStatus      旧状态
     * @return 更新记录数
     */
    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @Query(value = "update InstanceInfoDO set lastReportTime = :lastReportTime, gmtModified = :modifyTime, runningTimes = :runningTimes, status = :status  where instanceId = :instanceId and status = :oldStatus")
    int updateStatusChangeInfoByInstanceIdAndStatus(@Param("lastReportTime") long lastReportTime, @Param("modifyTime") Date modifyTime, @Param("runningTimes") long runningTimes, @Param("status") int status, @Param("instanceId") long instanceId, @Param("oldStatus") int oldStatus);

    /**
     * 更新任务执行记录内容（DispatchService专用）
     *
     * @param instanceId         实例 ID
     * @param status             状态
     * @param actualTriggerTime  实际调度时间
     * @param finishedTime       完成时间
     * @param taskTrackerAddress taskTracker 地址
     * @param result             结果
     * @param modifyTime         更新时间
     * @return 更新记录数量
     */
    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = :status, actualTriggerTime = :actualTriggerTime, finishedTime = :finishedTime, taskTrackerAddress = :taskTrackerAddress, result = :result,  gmtModified = :modifyTime where instanceId = :instanceId")
    int update4TriggerFailed(@Param("instanceId") long instanceId, @Param("status") int status, @Param("actualTriggerTime") long actualTriggerTime, @Param("finishedTime") long finishedTime, @Param("taskTrackerAddress") String taskTrackerAddress, @Param("result") String result, @Param("modifyTime") Date modifyTime);


    /**
     * 更新任务执行记录内容（DispatchService专用）
     *
     * @param instanceId         任务实例ID，分布式唯一
     * @param status             状态
     * @param actualTriggerTime  实际调度时间
     * @param taskTrackerAddress taskTracker 地址
     * @param modifyTime         更新时间
     * @param oldStatus          旧状态
     * @return 更新记录数量
     */
    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = :status,  actualTriggerTime = :actualTriggerTime, taskTrackerAddress = :taskTrackerAddress, gmtModified = :modifyTime where instanceId = :instanceId and status = :oldStatus")
    int update4TriggerSucceed(@Param("instanceId") long instanceId, @Param("status") int status, @Param("actualTriggerTime") long actualTriggerTime, @Param("taskTrackerAddress") String taskTrackerAddress, @Param("modifyTime") Date modifyTime, @Param("oldStatus") int oldStatus);


    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = :status, gmtModified = :modifyTime where instanceId = :instanceId and status = :originStatus ")
    int updateStatusAndGmtModifiedByInstanceIdAndOriginStatus(@Param("instanceId") long instanceId, @Param("originStatus") int originStatus, @Param("status") int status, @Param("modifyTime") Date modifyTime);


    @Transactional(rollbackOn = Exception.class)
    @Modifying
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = :status, gmtModified = :modifyTime where instanceId in (:instanceIdList) and status = :originStatus ")
    int updateStatusAndGmtModifiedByInstanceIdListAndOriginStatus(@Param("instanceIdList") List<Long> instanceIdList, @Param("originStatus") int originStatus, @Param("status") int status, @Param("modifyTime") Date modifyTime);

    /**
     * 更新固定频率任务的执行记录
     *
     * @param instanceId   任务实例ID，分布式唯一
     * @param status       状态
     * @param runningTimes 执行次数
     * @param modifyTime   更新时间
     * @return 更新记录数量
     */
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @CanIgnoreReturnValue
    @Query(value = "update InstanceInfoDO set status = :status, runningTimes = :runningTimes, gmtModified = :modifyTime where instanceId = :instanceId")
    int update4FrequentJob(@Param("instanceId") long instanceId, @Param("status") int status, @Param("runningTimes") long runningTimes, @Param("modifyTime") Date modifyTime);

    List<InstanceInfoDO> findAllByAppIdInAndStatusAndExpectedTriggerTimeLessThan(@Param("appIds") List<Long> appIds, @Param("status") int status, @Param("time") long time, Pageable pageable);

    @Query(value = "select new tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo(i.appId,i.id,i.jobId,i.instanceId) from InstanceInfoDO i where i.appId in (:appIds) and i.status = :status and i.actualTriggerTime < :time")
    List<BriefInstanceInfo> selectBriefInfoByAppIdInAndStatusAndActualTriggerTimeLessThan(@Param("appIds") List<Long> appIds, @Param("status") int status, @Param("time") long time, Pageable pageable);

    @Query(value = "select new tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo(i.appId,i.id,i.jobId,i.instanceId,i.runningTimes) from InstanceInfoDO i where i.appId in (:appIds) and i.status = :status and i.gmtModified < :time")
    List<BriefInstanceInfo> selectBriefInfoByAppIdInAndStatusAndGmtModifiedBefore(@Param("appIds") List<Long> appIds, @Param("status") int status, @Param("time") Date time, Pageable pageable);


    InstanceInfoDO findByInstanceId(long instanceId);

    /* --数据统计-- */

    @Query(value = "select count(*) from InstanceInfoDO where appId = ?1 and status = ?2")
    long countByAppIdAndStatus(long appId, int status);

    long countByAppIdAndStatusAndGmtCreateAfter(long appId, int status, Date time);

    @Query(value = "select distinct jobId from InstanceInfoDO where jobId in ?1 and status in ?2")
    List<Long> findByJobIdInAndStatusIn(List<Long> jobIds, List<Integer> status);

    /**
     * 删除历史数据，JPA自带的删除居然是根据ID循环删，2000条数据删了几秒，也太拉垮了吧...
     * 结果只能用 int 接收
     *
     * @param time   更新时间阈值
     * @param status 状态
     * @return 删除记录数
     */
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @Query(value = "delete from InstanceInfoDO where gmtModified < ?1 and status in ?2")
    int deleteAllByGmtModifiedBeforeAndStatusIn(Date time, List<Integer> status);
}
