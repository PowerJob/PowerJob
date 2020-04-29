package com.github.kfcfans.oms.server.service.timing.schedule;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.service.JobService;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 任务调度执行服务（调度 CRON 表达式的任务进行执行）
 * FIX_RATE和FIX_DELAY任务不需要被调度，创建后直接被派发到Worker执行，只需要失败重试机制（在InstanceStatusCheckService中完成）
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
    private IdGenerateService idGenerateService;
    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    @Resource
    private JobService jobService;

    private static final long SCHEDULE_RATE = 15000;

    @Async("omsTimingPool")
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
        // 清理不需要维护的数据
        WorkerManagerService.clean(allAppIds);

        // 调度 CRON 表达式 JOB
        try {
            scheduleCornJob(allAppIds);
        }catch (Exception e) {
            log.error("[JobScheduleService] schedule cron job failed.", e);
        }
        String cronTime = stopwatch.toString();
        stopwatch.reset().start();

        // 调度 秒级任务
        try {
            scheduleFrequentJob(allAppIds);
        }catch (Exception e) {
            log.error("[JobScheduleService] schedule frequent job failed.", e);
        }
        log.info("[JobScheduleService] cron schedule: {}, frequent schedule: {}.", cronTime, stopwatch.stop());
    }

    /**
     * 调度 CRON 表达式类型的任务
     */
    private void scheduleCornJob(List<Long> appIds) {


        long nowTime = System.currentTimeMillis();
        long timeThreshold = nowTime + 2 * SCHEDULE_RATE;
        Lists.partition(appIds, MAX_BATCH_NUM).forEach(partAppIds -> {

            try {

                // 查询条件：任务开启 + 使用CRON表达调度时间 + 指定appId + 即将需要调度执行
                List<JobInfoDO> jobInfos = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(partAppIds, JobStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), timeThreshold);

                if (CollectionUtils.isEmpty(jobInfos)) {
                    return;
                }

                // 1. 批量写日志表
                Map<Long, Long> jobId2InstanceId = Maps.newHashMap();
                log.info("[JobScheduleService] These cron jobs will be scheduled： {}.", jobInfos);

                List<InstanceInfoDO> executeLogs = Lists.newLinkedList();
                jobInfos.forEach(jobInfoDO -> {

                    InstanceInfoDO executeLog = new InstanceInfoDO();
                    executeLog.setJobId(jobInfoDO.getId());
                    executeLog.setAppId(jobInfoDO.getAppId());
                    executeLog.setInstanceId(idGenerateService.allocate());
                    executeLog.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
                    executeLog.setExpectedTriggerTime(jobInfoDO.getNextTriggerTime());
                    executeLog.setGmtCreate(new Date());
                    executeLog.setGmtModified(executeLog.getGmtCreate());

                    executeLogs.add(executeLog);

                    jobId2InstanceId.put(executeLog.getJobId(), executeLog.getInstanceId());
                });
                instanceInfoRepository.saveAll(executeLogs);
                instanceInfoRepository.flush();

                // 2. 推入时间轮中等待调度执行
                jobInfos.forEach(jobInfoDO ->  {

                    Long instanceId = jobId2InstanceId.get(jobInfoDO.getId());

                    long targetTriggerTime = jobInfoDO.getNextTriggerTime();
                    long delay = 0;
                    if (targetTriggerTime < nowTime) {
                        log.warn("[JobScheduleService] find a delayed Job: {}.", jobInfoDO);
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

                        Date benchmarkTime = new Date(jobInfoDO.getNextTriggerTime());
                        Date nextTriggerTime = cronExpression.getNextValidTimeAfter(benchmarkTime);

                        JobInfoDO updatedJobInfo = new JobInfoDO();
                        BeanUtils.copyProperties(jobInfoDO, updatedJobInfo);
                        updatedJobInfo.setNextTriggerTime(nextTriggerTime.getTime());
                        updatedJobInfo.setGmtModified(now);

                        updatedJobInfos.add(updatedJobInfo);
                    } catch (Exception e) {
                        log.error("[JobScheduleService] calculate next trigger time for job(jobId={}) failed.", jobInfoDO.getId(), e);
                    }
                });
                jobInfoRepository.saveAll(updatedJobInfos);
                jobInfoRepository.flush();


            }catch (Exception e) {
                log.error("[JobScheduleService] schedule cron job failed.", e);
            }
        });
    }

    private void scheduleFrequentJob(List<Long> appIds) {

        Lists.partition(appIds, MAX_BATCH_NUM).forEach(partAppIds -> {
            try {
                // 查询所有的秒级任务（只包含ID）
                List<Long> jobIds = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeIn(partAppIds, JobStatus.ENABLE.getV(), TimeExpressionType.frequentTypes);
                // 查询日志记录表中是否存在相关的任务
                List<Long> runningJobIdList = instanceInfoRepository.findByJobIdInAndStatusIn(jobIds, InstanceStatus.generalizedRunningStatus);
                Set<Long> runningJobIdSet = Sets.newHashSet(runningJobIdList);

                List<Long> notRunningJobIds = Lists.newLinkedList();
                jobIds.forEach(jobId -> {
                    if (!runningJobIdSet.contains(jobId)) {
                        notRunningJobIds.add(jobId);
                    }
                });

                if (CollectionUtils.isEmpty(notRunningJobIds)) {
                    return;
                }

                log.info("[JobScheduleService] These frequent jobs will be scheduled： {}.", notRunningJobIds);
                notRunningJobIds.forEach(jobId -> jobService.runJob(jobId, null));
            }catch (Exception e) {
                log.error("[JobScheduleService] schedule frequent job failed.", e);
            }
        });
    }
}
