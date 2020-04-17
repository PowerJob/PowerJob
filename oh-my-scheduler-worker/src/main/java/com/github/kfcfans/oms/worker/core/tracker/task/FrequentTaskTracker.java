package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.LRUCache;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 处理秒级任务（FIX_RATE/FIX_DELAY）的TaskTracker
 * FIX_RATE 直接由 ScheduledExecutorService 实现，精度高，推荐使用
 * FIX_DELAY 会有几秒的延迟，精度不是很理想
 *
 * @author tjq
 * @since 2020/4/8
 */
@Slf4j
public class FrequentTaskTracker extends TaskTracker {

    // 时间表达式类型
    private TimeExpressionType timeExpressionType;
    private long timeParams;
    // 最大同时运行实例数
    private int maxInstanceNum;

    // 总运行次数（正常情况不会出现锁竞争，直接用 Atomic 系列，锁竞争验证推荐 LongAdder）
    private AtomicLong triggerTimes;
    private AtomicLong succeedTimes;
    private AtomicLong failedTimes;

    // 任务发射器
    private Launcher launcher;
    // 保存最近10个子任务的信息，供用户查询（user -> server -> worker 传递查询）
    private LRUCache<Long, SubInstanceInfo> recentSubInstanceInfo;
    // 保存运行中的任务
    private Map<Long, SubInstanceTimeHolder> subInstanceId2TimeHolder;

    private static final int HISTORY_SIZE = 10;
    private static final String LAST_TASK_ID_PREFIX = "L";

    protected FrequentTaskTracker(ServerScheduleJobReq req) {
        super(req);
    }

    @Override
    protected void initTaskTracker(ServerScheduleJobReq req) {

        // 0. 初始化实例变量
        timeExpressionType = TimeExpressionType.valueOf(req.getTimeExpressionType());
        timeParams = Long.parseLong(req.getTimeExpression());
        maxInstanceNum = req.getMaxInstanceNum();

        triggerTimes = new AtomicLong(0);
        succeedTimes = new AtomicLong(0);
        failedTimes = new AtomicLong(0);

        recentSubInstanceInfo = new LRUCache<>(HISTORY_SIZE);
        subInstanceId2TimeHolder = Maps.newConcurrentMap();

        // 1. 初始化定时调度线程池
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("oms-TaskTrackerTimingPool-%d").build();
        this.scheduledPool = Executors.newScheduledThreadPool(3, factory);

        // 2. 启动任务发射器
        launcher = new Launcher();
        if (timeExpressionType == TimeExpressionType.FIX_RATE) {
            scheduledPool.scheduleAtFixedRate(launcher, 1, timeParams, TimeUnit.MILLISECONDS);
        }else {
            scheduledPool.schedule(launcher, 0, TimeUnit.MILLISECONDS);
        }

        // 3. 启动任务分发器（事实上，秒级任务应该都是单机任务，且感觉不需要失败重试机制，那么 Dispatcher 的存在就有点浪费系统资源了...）
        scheduledPool.scheduleWithFixedDelay(new Dispatcher(), 1, 2, TimeUnit.SECONDS);
        // 4. 启动状态检查器
        scheduledPool.scheduleWithFixedDelay(new Checker(), 5000, Math.min(timeParams, 10000), TimeUnit.MILLISECONDS);

    }

    @Override
    public InstanceDetail fetchRunningStatus() {
        InstanceDetail detail = new InstanceDetail();
        // 填充基础信息
        detail.setActualTriggerTime(createTime);
        detail.setStatus(InstanceStatus.RUNNING.getDes());
        detail.setTaskTrackerAddress(OhMyWorker.getWorkerAddress());

        List<InstanceDetail.SubInstanceDetail> history = Lists.newLinkedList();
        recentSubInstanceInfo.forEach((ignore, subInstanceInfo) -> {
            InstanceDetail.SubInstanceDetail subDetail = new InstanceDetail.SubInstanceDetail();
            BeanUtils.copyProperties(subInstanceInfo, subDetail);
            subDetail.setStatus(InstanceStatus.of(subInstanceInfo.status).getDes());

            history.add(subDetail);
        });

        detail.setExtra(history);
        return detail;
    }

    /**
     * 任务发射器（@Reference 饥荒->雪球发射器）
     */
    private class Launcher implements Runnable {

        public void innerRun() {

            // 子任务实例ID
            Long subInstanceId = triggerTimes.incrementAndGet();

            // 记录时间
            SubInstanceTimeHolder timeHolder = new SubInstanceTimeHolder();
            timeHolder.startTime = timeHolder.lastActiveTime = System.currentTimeMillis();
            subInstanceId2TimeHolder.put(subInstanceId, timeHolder);

            // 执行记录缓存
            SubInstanceInfo subInstanceInfo = new SubInstanceInfo();
            subInstanceInfo.status = TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue();
            subInstanceInfo.startTime = timeHolder.startTime;
            recentSubInstanceInfo.put(subInstanceId, subInstanceInfo);

            String myAddress = OhMyWorker.getWorkerAddress();
            String taskId = String.valueOf(subInstanceId);

            TaskDO newRootTask = new TaskDO();

            newRootTask.setInstanceId(instanceId);
            newRootTask.setSubInstanceId(subInstanceId);
            newRootTask.setTaskId(taskId);

            newRootTask.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
            newRootTask.setFailedCnt(0);
            // 根任务总是默认本机执行
            newRootTask.setAddress(myAddress);
            newRootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
            newRootTask.setCreatedTime(System.currentTimeMillis());
            newRootTask.setLastModifiedTime(System.currentTimeMillis());
            newRootTask.setLastReportTime(-1L);

            // 判断是否超出最大执行实例数
            if (timeExpressionType == TimeExpressionType.FIX_RATE) {
                if (subInstanceId2TimeHolder.size() > maxInstanceNum) {
                    log.warn("[TaskTracker-{}] cancel to launch the subInstance({}) due to too much subInstance is running.", instanceId, subInstanceId);
                    processFinishedSubInstance(subInstanceId, false, "TOO_MUCH_INSTANCE");
                    return;
                }
            }

            // 必须先持久化，持久化成功才能 dispatch，否则会导致后续报错（因为DB中没有这个taskId对应的记录，会各种报错）
            if (!taskPersistenceService.save(newRootTask)) {
                log.error("[TaskTracker-{}] Launcher create new root task failed.", instanceId);
                processFinishedSubInstance(subInstanceId, false, "LAUNCH_FAILED");
                return;
            }

            dispatchTask(newRootTask, myAddress);
        }

        @Override
        public void run() {
            try {
                innerRun();
            }catch (Exception e) {
                log.error("[TaskTracker-{}] launch task failed.", instanceId, e);
            }
        }
    }

    /**
     * 检查各个SubInstance的完成情况
     */
    private class Checker implements Runnable {

        private static final long HEARTBEAT_TIMEOUT_MS = 60000;

        @Override
        public void run() {
            try {
                checkStatus();
                reportStatus();
            }catch (Exception e) {
                log.warn("[TaskTracker-{}] check and report status failed.", instanceId, e);
            }
        }

        private void checkStatus() {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
            long instanceTimeoutMS = instanceInfo.getInstanceTimeoutMS();
            long nowTS = System.currentTimeMillis();

            Iterator<Map.Entry<Long, SubInstanceTimeHolder>> iterator = subInstanceId2TimeHolder.entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<Long, SubInstanceTimeHolder> entry = iterator.next();
                Long subInstanceId = entry.getKey();
                SubInstanceTimeHolder timeHolder = entry.getValue();

                long executeTimeout = nowTS - timeHolder.startTime;
                long heartbeatTimeout = nowTS - timeHolder.lastActiveTime;

                // 超时（包含总运行时间超时和心跳包超时），直接判定为失败
                if (executeTimeout > instanceTimeoutMS || heartbeatTimeout > HEARTBEAT_TIMEOUT_MS) {

                    onFinished(subInstanceId, false, "TIMEOUT", iterator);
                    continue;
                }

                // 查看执行情况
                InstanceStatisticsHolder holder = getInstanceStatisticsHolder(subInstanceId);

                long finishedNum = holder.succeedNum + holder.failedNum;
                long unfinishedNum = holder.waitingDispatchNum + holder.workerUnreceivedNum + holder.receivedNum + holder.runningNum;

                if (unfinishedNum == 0) {

                    // 数据库中没有该 subInstanceId 的记录，说明任务发射器写入DB失败，直接视为执行失败，删除数据
                    if (finishedNum == 0) {
                        onFinished(subInstanceId, false, "LAUNCH_FAILED", iterator);
                        continue;
                    }

                    String result;
                    switch (executeType) {
                        // STANDALONE 代表任务确实已经执行完毕了
                        case STANDALONE:
                            // 查询数据库获取结果（STANDALONE每个SubInstance只会有一条Task记录）
                            result = taskPersistenceService.getAllTask(instanceId, subInstanceId).get(0).getResult();
                            onFinished(subInstanceId, true, result, iterator);
                            continue;
                        // MAP 不关心结果，最简单
                        case MAP:
                            result = String.format("total:%d,succeed:%d,failed:%d", holder.getTotalTaskNum(), holder.succeedNum, holder.failedNum);
                            onFinished(subInstanceId, holder.failedNum == 0, result, iterator);
                            continue;
                        // MapReduce 和 BroadCast 需要根据是否有 LAST_TASK 来判断结束与否
                        default:
                            Optional<TaskDO> lastTaskOptional = taskPersistenceService.getLastTask(instanceId, subInstanceId);
                            if (lastTaskOptional.isPresent()) {

                                TaskStatus lastTaskStatus = TaskStatus.of(lastTaskOptional.get().getStatus());
                                if (lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS || lastTaskStatus == TaskStatus.WORKER_PROCESS_FAILED) {
                                    onFinished(subInstanceId, lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS, lastTaskOptional.get().getResult(), iterator);
                                }
                            }else {

                                // 创建最终任务并提交执行
                                TaskDO newLastTask = new TaskDO();
                                newLastTask.setTaskName(TaskConstant.LAST_TASK_NAME);
                                newLastTask.setTaskId(LAST_TASK_ID_PREFIX + subInstanceId);
                                newLastTask.setSubInstanceId(subInstanceId);
                                newLastTask.setAddress(OhMyWorker.getWorkerAddress());
                                submitTask(Lists.newArrayList(newLastTask));
                            }

                    }
                }

                // 舍去一切重试机制，反正超时就失败

                log.debug("[TaskTracker-{}] check status using {}.", instanceId, stopwatch.stop());
            }
        }

        private void reportStatus() {

            if (StringUtils.isEmpty(OhMyWorker.getCurrentServer())) {
                return;
            }

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setJobId(instanceInfo.getJobId());
            req.setInstanceId(instanceId);
            req.setReportTime(System.currentTimeMillis());
            req.setStartTime(createTime);
            req.setInstanceStatus(InstanceStatus.RUNNING.getV());

            req.setTotalTaskNum(triggerTimes.get());
            req.setSucceedTaskNum(succeedTimes.get());
            req.setFailedTaskNum(failedTimes.get());
            req.setSourceAddress(OhMyWorker.getWorkerAddress());

            String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
            if (StringUtils.isEmpty(serverPath)) {
                return;
            }
            // 非可靠通知，Server挂掉后任务的kill工作交由其他线程去做
            ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);
            serverActor.tell(req, null);
        }

        /**
         * 处理任务完成的情况，删除内存 & 数据库数据
         */
        private void onFinished(Long subInstanceId, boolean success, String result, Iterator<?> iterator) {
            iterator.remove();
            processFinishedSubInstance(subInstanceId, success, result);
        }
    }

    private void processFinishedSubInstance(long subInstanceId, boolean success, String result) {
        if (success) {
            succeedTimes.incrementAndGet();
        } else {
            failedTimes.incrementAndGet();
        }

        // 从运行中任务列表移除
        subInstanceId2TimeHolder.remove(subInstanceId);

        // 更新缓存数据
        if (recentSubInstanceInfo.containsKey(subInstanceId)) {
            SubInstanceInfo subInstanceInfo = recentSubInstanceInfo.get(subInstanceId);
            subInstanceInfo.status = success ? InstanceStatus.SUCCEED.getV() : InstanceStatus.FAILED.getV();
            subInstanceInfo.result = result;
            subInstanceInfo.finishedTime = System.currentTimeMillis();
        }
        // 删除数据库相关数据
        taskPersistenceService.deleteAllSubInstanceTasks(instanceId, subInstanceId);

        // FIX_DELAY 则调度下次任务
        if (timeExpressionType == TimeExpressionType.FIX_DELAY) {
            scheduledPool.schedule(launcher, timeParams, TimeUnit.MILLISECONDS);
        }
    }

    @Data
    private static class SubInstanceInfo {
        private int status;
        private long startTime;
        private long finishedTime;
        private String result;
    }

    private static class SubInstanceTimeHolder {
        private long startTime;
        private long lastActiveTime;
    }

}
