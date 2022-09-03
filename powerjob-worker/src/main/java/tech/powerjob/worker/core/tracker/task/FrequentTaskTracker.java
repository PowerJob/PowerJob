package tech.powerjob.worker.core.tracker.task;

import akka.actor.ActorSelection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.AlarmConfig;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.common.utils.LRUCache;
import tech.powerjob.worker.persistence.TaskDO;

import java.util.*;
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

    /**
     * 时间表达式类型
     */
    private TimeExpressionType timeExpressionType;

    private long timeParams;
    /**
     * 最大同时运行实例数
     */
    private int maxInstanceNum;

    /**
     * 总运行次数（正常情况不会出现锁竞争，直接用 Atomic 系列，锁竞争严重推荐 LongAdder）
     */
    private AtomicLong triggerTimes;

    private AtomicLong succeedTimes;

    private AtomicLong failedTimes;
    /**
     * 任务发射器
     */
    private Launcher launcher;
    /**
     * 保存最近10个子任务的信息，供用户查询（user -> server -> worker 传递查询）
     */
    private LRUCache<Long, SubInstanceInfo> recentSubInstanceInfo;
    /**
     * 保存运行中的任务
     */
    private Map<Long, SubInstanceTimeHolder> subInstanceId2TimeHolder;

    private AlertManager alertManager;

    private static final int HISTORY_SIZE = 10;
    private static final String LAST_TASK_ID_PREFIX = "L";
    private static final int MIN_INTERVAL = 50;

    protected FrequentTaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        super(req, workerRuntime);
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
        String poolName = String.format("ftttp-%d", req.getInstanceId()) + "-%d";
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat(poolName).build();
        this.scheduledPool = Executors.newScheduledThreadPool(4, factory);
        this.alertManager = constructAlertManager(req);
        // 2. 启动任务发射器
        launcher = new Launcher();
        if (timeExpressionType == TimeExpressionType.FIXED_RATE) {
            // 固定频率需要设置最小间隔
            if (timeParams < MIN_INTERVAL) {
                throw new PowerJobException("time interval too small, please set the timeExpressionInfo >= 1000");
            }
            scheduledPool.scheduleAtFixedRate(launcher, 1, timeParams, TimeUnit.MILLISECONDS);
        } else {
            scheduledPool.schedule(launcher, 0, TimeUnit.MILLISECONDS);
        }

        // 3. 启动任务分发器（事实上，秒级任务应该都是单机任务，且感觉不需要失败重试机制，那么 Dispatcher 的存在就有点浪费系统资源了...）
        scheduledPool.scheduleWithFixedDelay(new Dispatcher(), 1, 2, TimeUnit.SECONDS);
        // 4. 启动状态检查器
        scheduledPool.scheduleWithFixedDelay(new Checker(), 5000, Math.min(Math.max(timeParams, 5000), 15000), TimeUnit.MILLISECONDS);
        // 5. 启动执行器动态检测装置
        scheduledPool.scheduleAtFixedRate(new WorkerDetector(), 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public InstanceDetail fetchRunningStatus() {
        InstanceDetail detail = new InstanceDetail();
        // 填充基础信息
        detail.setActualTriggerTime(createTime);
        detail.setStatus(InstanceStatus.RUNNING.getV());
        detail.setTaskTrackerAddress(workerRuntime.getWorkerAddress());

        List<InstanceDetail.SubInstanceDetail> history = Lists.newLinkedList();
        recentSubInstanceInfo.forEach((subId, subInstanceInfo) -> {
            InstanceDetail.SubInstanceDetail subDetail = new InstanceDetail.SubInstanceDetail();
            BeanUtils.copyProperties(subInstanceInfo, subDetail);
            InstanceStatus status = InstanceStatus.of(subInstanceInfo.status);
            subDetail.setStatus(status.getV());
            subDetail.setSubInstanceId(subId);

            history.add(subDetail);
        });

        // 按 subInstanceId 排序 issue#63
        history.sort((o1, o2) -> (int) (o2.getSubInstanceId() - o1.getSubInstanceId()));

        detail.setSubInstanceDetails(history);
        return detail;
    }

    /**
     * 任务发射器（@Reference 饥荒->雪球发射器）
     */
    private class Launcher implements Runnable {

        public void innerRun() {

            if (finished.get()) {
                return;
            }

            // 子任务实例ID
            Long subInstanceId = triggerTimes.incrementAndGet();

            // 执行记录缓存（只做展示，因此可以放在前面）
            SubInstanceInfo subInstanceInfo = new SubInstanceInfo();
            subInstanceInfo.status = TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue();
            subInstanceInfo.startTime = System.currentTimeMillis();
            recentSubInstanceInfo.put(subInstanceId, subInstanceInfo);

            String myAddress = workerRuntime.getWorkerAddress();
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
            if (maxInstanceNum > 0) {
                if (timeExpressionType == TimeExpressionType.FIXED_RATE) {
                    if (subInstanceId2TimeHolder.size() >= maxInstanceNum) {
                        log.warn("[FQTaskTracker-{}] cancel to launch the subInstance({}) due to too much subInstance is running.", instanceId, subInstanceId);
                        processFinishedSubInstance(subInstanceId, false, "TOO_MUCH_INSTANCE");
                        return;
                    }
                }
            }

            // 必须先持久化，持久化成功才能 dispatch，否则会导致后续报错（因为DB中没有这个taskId对应的记录，会各种报错）
            if (!taskPersistenceService.save(newRootTask)) {
                log.error("[FQTaskTracker-{}] Launcher create new root task failed.", instanceId);
                processFinishedSubInstance(subInstanceId, false, "LAUNCH_FAILED");
                return;
            }

            // 生成记录信息（必须保证持久化成功才能生成该记录，否则会导致 LAUNCH_FAILED 错误）
            SubInstanceTimeHolder timeHolder = new SubInstanceTimeHolder();
            timeHolder.startTime = System.currentTimeMillis();
            subInstanceId2TimeHolder.put(subInstanceId, timeHolder);

            dispatchTask(newRootTask, myAddress);
        }

        @Override
        public void run() {
            try {
                innerRun();
            } catch (Exception e) {
                log.error("[FQTaskTracker-{}] launch task failed.", instanceId, e);
            }
        }
    }

    /**
     * 检查各个SubInstance的完成情况
     */
    private class Checker implements Runnable {

        @Override
        public void run() {

            if (finished.get()) {
                return;
            }

            try {
                checkStatus();
                reportStatus();
            } catch (Exception e) {
                log.warn("[FQTaskTracker-{}] check and report status failed.", instanceId, e);
            }
        }

        private void checkStatus() {
            Stopwatch stopwatch = Stopwatch.createStarted();

            // worker 挂掉的任务直接置为失败
            List<String> disconnectedPTs = ptStatusHolder.getAllDisconnectedProcessorTrackers();
            if (!disconnectedPTs.isEmpty()) {
                log.warn("[FQTaskTracker-{}] some ProcessorTracker disconnected from TaskTracker,their address is {}.", instanceId, disconnectedPTs);
                if (taskPersistenceService.updateLostTasks(instanceId, disconnectedPTs, false)) {
                    ptStatusHolder.remove(disconnectedPTs);
                    log.warn("[FQTaskTracker-{}] removed these ProcessorTracker from StatusHolder: {}", instanceId, disconnectedPTs);
                }
            }

            ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
            long instanceTimeoutMS = instanceInfo.getInstanceTimeoutMS();
            long nowTS = System.currentTimeMillis();

            Iterator<Map.Entry<Long, SubInstanceTimeHolder>> iterator = subInstanceId2TimeHolder.entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<Long, SubInstanceTimeHolder> entry = iterator.next();
                Long subInstanceId = entry.getKey();
                SubInstanceTimeHolder timeHolder = entry.getValue();

                long executeTimeout = nowTS - timeHolder.startTime;

                // 超时（包含总运行时间超时和心跳包超时），直接判定为失败
                if (executeTimeout > instanceTimeoutMS) {
                    onFinished(subInstanceId, false, "RUNNING_TIMEOUT", iterator);
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

                    switch (executeType) {
                        // STANDALONE 代表任务确实已经执行完毕了
                        case STANDALONE:
                            // 查询数据库获取结果（STANDALONE每个SubInstance只会有一条Task记录）
                            TaskDO resultTask = taskPersistenceService.getAllTask(instanceId, subInstanceId).get(0);
                            boolean success = resultTask.getStatus() == TaskStatus.WORKER_PROCESS_SUCCESS.getValue();
                            onFinished(subInstanceId, success, resultTask.getResult(), iterator);
                            continue;
                            // MAP 不关心结果，最简单
                        case MAP:
                            String result = String.format("total:%d,succeed:%d,failed:%d", holder.getTotalTaskNum(), holder.succeedNum, holder.failedNum);
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
                            } else {

                                // 创建最终任务并提交执行
                                TaskDO newLastTask = new TaskDO();
                                newLastTask.setTaskName(TaskConstant.LAST_TASK_NAME);
                                newLastTask.setTaskId(LAST_TASK_ID_PREFIX + subInstanceId);
                                newLastTask.setSubInstanceId(subInstanceId);
                                newLastTask.setAddress(workerRuntime.getWorkerAddress());
                                submitTask(Lists.newArrayList(newLastTask));
                            }
                    }
                }
                // 舍去一切重试机制，反正超时就失败
            }
            log.debug("[FQTaskTracker-{}] check status using {}.", instanceId, stopwatch);
        }

        private void reportStatus() {

            String currentServerAddress = workerRuntime.getServerDiscoveryService().getCurrentServerAddress();
            if (StringUtils.isEmpty(currentServerAddress)) {
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
            req.setSourceAddress(workerRuntime.getWorkerAddress());

            // alert
            if (alertManager.alert()) {
                req.setNeedAlert(true);
                req.setAlertContent(alertManager.getAlertContent());
                log.warn("[FQTaskTracker-{}] report alert req,time:{}", instanceId, req.getReportTime());
            }

            String serverPath = AkkaUtils.getServerActorPath(currentServerAddress);
            if (StringUtils.isEmpty(serverPath)) {
                return;
            }
            // 非可靠通知，Server挂掉后任务的kill工作交由其他线程去做
            ActorSelection serverActor = workerRuntime.getActorSystem().actorSelection(serverPath);
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
        long currentTime = System.currentTimeMillis();
        if (success) {
            succeedTimes.incrementAndGet();
        } else {
            failedTimes.incrementAndGet();
            alertManager.update(currentTime, result);
        }

        // 从运行中任务列表移除
        subInstanceId2TimeHolder.remove(subInstanceId);

        // 更新缓存数据
        SubInstanceInfo subInstanceInfo = recentSubInstanceInfo.get(subInstanceId);
        if (subInstanceInfo != null) {
            subInstanceInfo.status = success ? InstanceStatus.SUCCEED.getV() : InstanceStatus.FAILED.getV();
            subInstanceInfo.result = result;
            subInstanceInfo.finishedTime = currentTime;
        }
        // 删除数据库相关数据
        taskPersistenceService.deleteAllSubInstanceTasks(instanceId, subInstanceId);

        // FIX_DELAY 则调度下次任务
        if (timeExpressionType == TimeExpressionType.FIXED_DELAY) {
            scheduledPool.schedule(launcher, timeParams, TimeUnit.MILLISECONDS);
        }
    }


    private AlertManager constructAlertManager(ServerScheduleJobReq req) {

        long rate = Long.parseLong(req.getTimeExpression());
        if (!StringUtils.isEmpty(req.getAlarmConfig())) {
            try {
                log.debug("[FQTaskTracker-{}] alert config:{}", instanceId, req.getAlarmConfig());
                AlarmConfig alarmConfig = JsonUtils.parseObject(req.getAlarmConfig(), AlarmConfig.class);
                return new AlertManager(alarmConfig);
            } catch (JsonProcessingException ignore) {
                //
            }
        }
        // 默认配置，失败一次就告警，沉默窗口 5 分钟
        int statisticWindowLen = Math.max((int) (2 * rate / 1000), 1);
        return new AlertManager(new AlarmConfig(1, statisticWindowLen, 300));
    }

    private class AlertManager {
        /**
         * 记录执行失败的时间
         */
        private final LinkedList<Long> failedRecordList = new LinkedList<>();
        /**
         * 告警配置
         */
        private final AlarmConfig config;
        /**
         * 告警的激活时间
         */
        private long alarmActiveTime = 0L;
        /**
         * 告警的内容（记录最后一次失败的任务执行结果）
         */
        private String content;
        /**
         * 是否处于激活状态
         */
        private boolean active;


        public AlertManager(AlarmConfig config) {
            log.info("[FQTaskTracker-{}] create alert manager,alertThreshold:{},statisticWindowLen:{} s,silenceWindowLen:{} s", instanceId, config.getAlertThreshold(), config.getStatisticWindowLen(), config.getSilenceWindowLen());
            this.config = config;
        }

        public synchronized void update(long currentTime, String result) {
            log.debug("[FQTaskTracker-{}] update alert statistic info,currentTime:{}", instanceId, currentTime);
            if (currentTime < alarmActiveTime + config.getSilenceWindowLen() * 1000) {
                // 处于沉默窗口内
                return;
            }
            // 当前统计窗口允许的最小值
            long minTime = currentTime - config.getStatisticWindowLen() * 1000;
            while (!failedRecordList.isEmpty() && failedRecordList.peekFirst() < minTime) {
                failedRecordList.removeFirst();
            }
            failedRecordList.add(currentTime);
            if (failedRecordList.size() >= config.getAlertThreshold()) {
                // 标记需要告警
                active = true;
                alarmActiveTime = currentTime;
                content = result;
            }
        }


        public synchronized boolean alert() {
            if (active) {
                active = false;
                return true;
            }
            return false;
        }

        public String getAlertContent() {
            return content;
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
    }

}
