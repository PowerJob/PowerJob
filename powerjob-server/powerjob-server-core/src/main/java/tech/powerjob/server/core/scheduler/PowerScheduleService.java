package tech.powerjob.server.core.scheduler;

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
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import tech.powerjob.server.common.utils.TimeUtils;
import tech.powerjob.server.core.DispatchService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.service.JobService;
import tech.powerjob.server.core.workflow.WorkflowInstanceManager;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.remote.transport.starter.AkkaStarter;
import tech.powerjob.server.remote.worker.WorkerClusterManagerService;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务调度执行服务（调度 CRON 表达式的任务进行执行）
 * 原：FIX_RATE和FIX_DELAY任务不需要被调度，创建后直接被派发到Worker执行，只需要失败重试机制（在InstanceStatusCheckService中完成）
 * 先：那样写不太优雅，东一坨代码西一坨代码的，还是牺牲点性能统一调度算了 （优雅，永不过时～ BY：青钢影）
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class PowerScheduleService {

    /**
     * 每次并发调度的应用数量
     */
    private static final int MAX_APP_NUM = 10;

    @Resource
    private DispatchService dispatchService;
    @Resource
    private InstanceService instanceService;
    @Resource
    private WorkflowInstanceManager workflowInstanceManager;

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    @Resource
    private JobService jobService;

    private static final long SCHEDULE_RATE = 15000;

    @Async("omsTimingPool")
    @Scheduled(fixedDelay = SCHEDULE_RATE)
    public void timingSchedule() {

        long start = System.currentTimeMillis();
        Stopwatch stopwatch = Stopwatch.createStarted();

        // 先查询DB，查看本机需要负责的任务
        List<AppInfoDO> allAppInfos = appInfoRepository.findAllByCurrentServer(AkkaStarter.getActorSystemAddress());
        if (CollectionUtils.isEmpty(allAppInfos)) {
            log.info("[JobScheduleService] current server has no app's job to schedule.");
            return;
        }
        List<Long> allAppIds = allAppInfos.stream().map(AppInfoDO::getId).collect(Collectors.toList());
        // 清理不需要维护的数据
        WorkerClusterManagerService.clean(allAppIds);

        // 调度 CRON 表达式 JOB
        try {
            scheduleCronJob(allAppIds);
        } catch (Exception e) {
            log.error("[CronScheduler] schedule cron job failed.", e);
        }
        String cronTime = stopwatch.toString();
        stopwatch.reset().start();

        // 调度 workflow 任务
        try {
            scheduleWorkflow(allAppIds);
        } catch (Exception e) {
            log.error("[WorkflowScheduler] schedule workflow job failed.", e);
        }
        String wfTime = stopwatch.toString();
        stopwatch.reset().start();

        // 调度 秒级任务
        try {
            scheduleFrequentJob(allAppIds);
        } catch (Exception e) {
            log.error("[FrequentScheduler] schedule frequent job failed.", e);
        }

        log.info("[JobScheduleService] cron schedule: {}, workflow schedule: {}, frequent schedule: {}.", cronTime, wfTime, stopwatch.stop());

        long cost = System.currentTimeMillis() - start;
        if (cost > SCHEDULE_RATE) {
            log.warn("[JobScheduleService] The database query is using too much time({}ms), please check if the database load is too high!", cost);
        }
    }

    /**
     * 调度 CRON 表达式类型的任务
     */
    private void scheduleCronJob(List<Long> appIds) {

        long nowTime = System.currentTimeMillis();
        long timeThreshold = nowTime + 2 * SCHEDULE_RATE;
        Lists.partition(appIds, MAX_APP_NUM).forEach(partAppIds -> {

            try {

                // 查询条件：任务开启 + 使用CRON表达调度时间 + 指定appId + 即将需要调度执行
                List<JobInfoDO> jobInfos = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(partAppIds, SwitchableStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), timeThreshold);

                if (CollectionUtils.isEmpty(jobInfos)) {
                    return;
                }

                // 1. 批量写日志表
                Map<Long, Long> jobId2InstanceId = Maps.newHashMap();
                log.info("[CronScheduler] These cron jobs will be scheduled: {}.", jobInfos);

                jobInfos.forEach(jobInfo -> {
                    Long instanceId = instanceService.create(jobInfo.getId(), jobInfo.getAppId(), jobInfo.getJobParams(), null, null, jobInfo.getNextTriggerTime());
                    jobId2InstanceId.put(jobInfo.getId(), instanceId);
                });
                instanceInfoRepository.flush();

                // 2. 推入时间轮中等待调度执行
                jobInfos.forEach(jobInfoDO -> {

                    Long instanceId = jobId2InstanceId.get(jobInfoDO.getId());

                    long targetTriggerTime = jobInfoDO.getNextTriggerTime();
                    long delay = 0;
                    if (targetTriggerTime < nowTime) {
                        log.warn("[Job-{}] schedule delay, expect: {}, current: {}", jobInfoDO.getId(), targetTriggerTime, System.currentTimeMillis());
                    } else {
                        delay = targetTriggerTime - nowTime;
                    }

                    InstanceTimeWheelService.schedule(instanceId, delay, () -> dispatchService.dispatch(jobInfoDO, instanceId));
                });

                // 3. 计算下一次调度时间（忽略5S内的重复执行，即CRON模式下最小的连续执行间隔为 SCHEDULE_RATE ms）
                jobInfos.forEach(jobInfoDO -> {
                    try {
                        refreshJob(jobInfoDO);
                    } catch (Exception e) {
                        log.error("[Job-{}] refresh job failed.", jobInfoDO.getId(), e);
                    }
                });
                jobInfoRepository.flush();


            } catch (Exception e) {
                log.error("[CronScheduler] schedule cron job failed.", e);
            }
        });
    }

    private void scheduleWorkflow(List<Long> appIds) {

        long nowTime = System.currentTimeMillis();
        long timeThreshold = nowTime + 2 * SCHEDULE_RATE;
        Lists.partition(appIds, MAX_APP_NUM).forEach(partAppIds -> {
            List<WorkflowInfoDO> wfInfos = workflowInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(partAppIds, SwitchableStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), timeThreshold);

            if (CollectionUtils.isEmpty(wfInfos)) {
                return;
            }

            wfInfos.forEach(wfInfo -> {

                // 1. 先生成调度记录，防止不调度的情况发生
                Long wfInstanceId = workflowInstanceManager.create(wfInfo, null, wfInfo.getNextTriggerTime());

                // 2. 推入时间轮，准备调度执行
                long delay = wfInfo.getNextTriggerTime() - System.currentTimeMillis();
                if (delay < 0) {
                    log.warn("[Workflow-{}] workflow schedule delay, expect:{}, actual: {}", wfInfo.getId(), wfInfo.getNextTriggerTime(), System.currentTimeMillis());
                    delay = 0;
                }
                InstanceTimeWheelService.schedule(wfInstanceId, delay, () -> workflowInstanceManager.start(wfInfo, wfInstanceId));

                // 3. 重新计算下一次调度时间并更新
                try {
                    refreshWorkflow(wfInfo);
                } catch (Exception e) {
                    log.error("[Workflow-{}] refresh workflow failed.", wfInfo.getId(), e);
                }
            });
            workflowInfoRepository.flush();
        });
    }

    private void scheduleFrequentJob(List<Long> appIds) {

        Lists.partition(appIds, MAX_APP_NUM).forEach(partAppIds -> {
            try {
                // 查询所有的秒级任务（只包含ID）
                List<Long> jobIds = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeIn(partAppIds, SwitchableStatus.ENABLE.getV(), TimeExpressionType.frequentTypes);
                if (CollectionUtils.isEmpty(jobIds)) {
                    return;
                }
                // 查询日志记录表中是否存在相关的任务
                List<Long> runningJobIdList = instanceInfoRepository.findByJobIdInAndStatusIn(jobIds, InstanceStatus.GENERALIZED_RUNNING_STATUS);
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

                log.info("[FrequentScheduler] These frequent jobs will be scheduled： {}.", notRunningJobIds);
                notRunningJobIds.forEach(jobId -> {
                    Optional<JobInfoDO> jobInfoOpt = jobInfoRepository.findById(jobId);
                    jobInfoOpt.ifPresent(jobInfoDO -> jobService.runJob(jobInfoDO.getAppId(), jobId, null, 0L));
                });
            } catch (Exception e) {
                log.error("[FrequentScheduler] schedule frequent job failed.", e);
            }
        });
    }

    private void refreshJob(JobInfoDO jobInfo) throws ParseException {
        Date nextTriggerTime = calculateNextTriggerTime(jobInfo.getNextTriggerTime(), jobInfo.getTimeExpression(), jobInfo.getLifecycle());

        JobInfoDO updatedJobInfo = new JobInfoDO();
        BeanUtils.copyProperties(jobInfo, updatedJobInfo);

        if (nextTriggerTime == null) {
            log.warn("[Job-{}] this job won't be scheduled anymore, system will set the status to DISABLE!", jobInfo.getId());
            updatedJobInfo.setStatus(SwitchableStatus.DISABLE.getV());
        } else {
            updatedJobInfo.setNextTriggerTime(nextTriggerTime.getTime());
        }
        updatedJobInfo.setGmtModified(new Date());

        jobInfoRepository.save(updatedJobInfo);
    }

    private void refreshWorkflow(WorkflowInfoDO wfInfo) throws ParseException {
        Date nextTriggerTime = calculateNextTriggerTime(wfInfo.getNextTriggerTime(), wfInfo.getTimeExpression(), wfInfo.getLifecycle());

        WorkflowInfoDO updateEntity = new WorkflowInfoDO();
        BeanUtils.copyProperties(wfInfo, updateEntity);

        if (nextTriggerTime == null) {
            log.warn("[Workflow-{}] this workflow won't be scheduled anymore, system will set the status to DISABLE!", wfInfo.getId());
            wfInfo.setStatus(SwitchableStatus.DISABLE.getV());
        } else {
            updateEntity.setNextTriggerTime(nextTriggerTime.getTime());
        }

        updateEntity.setGmtModified(new Date());
        workflowInfoRepository.save(updateEntity);
    }

    /**
     * 计算下次触发时间
     *
     * @param preTriggerTime 前一次触发时间
     * @param cronExpression CRON 表达式
     * @return 下一次调度时间
     * @throws ParseException 异常
     */
    private static Date calculateNextTriggerTime(Long preTriggerTime, String cronExpression, String lifecycle) throws ParseException {

        // 取最大值，防止长时间未调度任务被连续调度（原来DISABLE的任务突然被打开，不取最大值会补上过去所有的调度）
        long benchmarkTime = Math.max(System.currentTimeMillis(), preTriggerTime);
        return TimeUtils.calculateNextCronTime(cronExpression, benchmarkTime, lifecycle);
    }
}
