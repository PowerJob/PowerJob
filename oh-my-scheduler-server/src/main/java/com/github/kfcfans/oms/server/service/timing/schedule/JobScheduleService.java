package com.github.kfcfans.oms.server.service.timing.schedule;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.core.InstanceManager;
import com.github.kfcfans.oms.server.core.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.IdGenerateService;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 任务调度执行服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class JobScheduleService {

    private static final int MAX_BATCH_NUM = 10;

    @Resource
    private DispatchService dispatchService;

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private ExecuteLogRepository executeLogRepository;

    private static final long SCHEDULE_RATE = 5000;

    @Scheduled(fixedRate = SCHEDULE_RATE)
    public void timingSchedule() {

        Stopwatch stopwatch = Stopwatch.createStarted();

        // 先查询DB，查看本机需要负责的任务
        List<AppInfoDO> allAppInfos = appInfoRepository.findAllByCurrentServer(OhMyServer.getActorSystemAddress());
        if (CollectionUtils.isEmpty(allAppInfos)) {
            log.info("[JobScheduleService] current server has no app's job to schedule.");
            return;
        }
        List<Long> allAppIds = allAppInfos.stream().map(AppInfoDO::getId).collect(Collectors.toList());

        // 调度 CRON 表达式 JOB
        try {
            scheduleCornJob(allAppIds);
        }catch (Exception e) {
            log.error("[JobScheduleService] schedule cron job failed.", e);
        }

        // 调度 FIX_RATE 和 FIX_DELAY JOB
        try {
            scheduleFrequentJob(allAppIds);
        }catch (Exception e) {
            log.error("[JobScheduleService] schedule frequent job failed.", e);
        }
        log.info("[JobScheduleService] finished job schedule, using time {}.", stopwatch.stop());
    }

    /**
     * 调度 CRON 表达式类型的任务
     */
    private void scheduleCornJob(List<Long> appIds) {

        // 清理不需要维护的数据
        WorkerManagerService.clean(appIds);

        long nowTime = System.currentTimeMillis();
        long timeThreshold = nowTime + 2 * SCHEDULE_RATE;
        Lists.partition(appIds, MAX_BATCH_NUM).forEach(partAppIds -> {

            try {

                // 查询条件：任务开启 + 使用CRON表达调度时间 + 指定appId + 即将需要调度执行
                List<JobInfoDO> jobInfos = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(partAppIds, JobStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), timeThreshold);

                if (CollectionUtils.isEmpty(jobInfos)) {
                    log.info("[JobScheduleService] no cron job need to schedule");
                    return;
                }

                // 1. 批量写日志表
                Map<Long, Long> jobId2InstanceId = Maps.newHashMap();
                log.info("[JobScheduleService] try to schedule some cron jobs, they are {}.", jobInfos);

                List<ExecuteLogDO> executeLogs = Lists.newLinkedList();
                jobInfos.forEach(jobInfoDO -> {

                    ExecuteLogDO executeLog = new ExecuteLogDO();
                    executeLog.setJobId(jobInfoDO.getId());
                    executeLog.setInstanceId(IdGenerateService.allocate());
                    executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
                    executeLog.setExpectedTriggerTime(jobInfoDO.getNextTriggerTime());
                    executeLog.setGmtCreate(new Date());
                    executeLog.setGmtModified(executeLog.getGmtCreate());

                    executeLogs.add(executeLog);

                    jobId2InstanceId.put(executeLog.getJobId(), executeLog.getInstanceId());
                });
                executeLogRepository.saveAll(executeLogs);
                executeLogRepository.flush();

                // 2. 推入时间轮中等待调度执行
                jobInfos.forEach(jobInfoDO ->  {

                    Long instanceId = jobId2InstanceId.get(jobInfoDO.getId());
                    // 注册到任务实例管理中心
                    InstanceManager.register(instanceId, jobInfoDO);

                    long targetTriggerTime = jobInfoDO.getNextTriggerTime();
                    long delay = 0;
                    if (targetTriggerTime < nowTime) {
                        log.warn("[JobScheduleService] Job({}) was delayed.", jobInfoDO);
                    }else {
                        delay = targetTriggerTime - nowTime;
                    }

                    HashedWheelTimerHolder.TIMER.schedule(() -> {
                        dispatchService.dispatch(jobInfoDO, instanceId, 0);
                    }, delay, TimeUnit.MILLISECONDS);
                });

                // 3. 计算下一次调度时间（忽略5S内的重复执行，即CRON模式下最小的连续执行间隔为 SCHEDULE_RATE ms）
                Date now = new Date();
                List<JobInfoDO> updatedJobInfos = Lists.newLinkedList();
                jobInfos.forEach(jobInfoDO -> {

                    try {
                        CronExpression cronExpression = new CronExpression(jobInfoDO.getTimeExpression());
                        Date nextTriggerTime = cronExpression.getNextValidTimeAfter(now);

                        JobInfoDO updatedJobInfo = new JobInfoDO();
                        BeanUtils.copyProperties(jobInfoDO, updatedJobInfo);
                        updatedJobInfo.setNextTriggerTime(nextTriggerTime.getTime());
                        updatedJobInfo.setGmtModified(now);

                        updatedJobInfos.add(updatedJobInfo);
                    } catch (Exception e) {
                        log.error("[JobScheduleService] calculate next trigger time for job({}) failed.", jobInfoDO, e);
                    }
                });
                jobInfoRepository.saveAll(updatedJobInfos);
                jobInfoRepository.flush();


            }catch (Exception e) {
                log.error("[JobScheduleService] schedule job failed.", e);
            }
        });
    }

    /**
     * 调度 FIX_RATE 和 FIX_DELAY 的任务
     */
    private void scheduleFrequentJob(List<Long> appIds) {

        List<JobInfoDO> fixDelayJobs = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionType(appIds, JobStatus.ENABLE.getV(), TimeExpressionType.FIX_DELAY.getV());
        List<JobInfoDO> fixRateJobs = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionType(appIds, JobStatus.ENABLE.getV(), TimeExpressionType.FIX_RATE.getV());

        List<Long> jobIds = Lists.newLinkedList();
        Map<Long, JobInfoDO> jobId2JobInfo = Maps.newHashMap();
        Consumer<JobInfoDO> consumer = jobInfo -> {
            jobIds.add(jobInfo.getId());
            jobId2JobInfo.put(jobInfo.getId(), jobInfo);
        };
        fixDelayJobs.forEach(consumer);
        fixRateJobs.forEach(consumer);

        if (CollectionUtils.isEmpty(jobIds)) {
            log.info("[JobScheduleService] no frequent job need to schedule for appIds in {}.", appIds);
            return;
        }

        // 查询 ExecuteLog 表，不存在或非运行状态则重新调度
        List<ExecuteLogDO> executeLogDOS = executeLogRepository.findByJobIdIn(jobIds);
        executeLogDOS.forEach(executeLogDO -> {
            if (executeLogDO.getStatus() == InstanceStatus.RUNNING.getV()) {
                jobId2JobInfo.remove(executeLogDO.getJobId());
            }
        });

        // 重新Dispatch
        jobId2JobInfo.values().forEach(jobInfoDO -> {

        });
    }
}
