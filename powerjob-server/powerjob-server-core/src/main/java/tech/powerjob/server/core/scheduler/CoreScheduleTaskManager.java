package tech.powerjob.server.core.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.TimeExpressionType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Echo009
 * @since 2022/10/12
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CoreScheduleTaskManager implements InitializingBean, DisposableBean {


    private final PowerScheduleService powerScheduleService;

    private final InstanceStatusCheckService instanceStatusCheckService;

    private final List<Thread> coreThreadContainer = new ArrayList<>();


    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    @Override
    public void afterPropertiesSet() {
        // 定时调度
        coreThreadContainer.add(new Thread(new LoopRunnable("ScheduleCronJob", PowerScheduleService.SCHEDULE_RATE, () -> powerScheduleService.scheduleNormalJob(TimeExpressionType.CRON)), "Thread-ScheduleCronJob"));
        coreThreadContainer.add(new Thread(new LoopRunnable("ScheduleDailyTimeIntervalJob", PowerScheduleService.SCHEDULE_RATE, () -> powerScheduleService.scheduleNormalJob(TimeExpressionType.DAILY_TIME_INTERVAL)), "Thread-ScheduleDailyTimeIntervalJob"));
        coreThreadContainer.add(new Thread(new LoopRunnable("ScheduleCronWorkflow", PowerScheduleService.SCHEDULE_RATE, powerScheduleService::scheduleCronWorkflow), "Thread-ScheduleCronWorkflow"));
        coreThreadContainer.add(new Thread(new LoopRunnable("ScheduleFrequentJob", PowerScheduleService.SCHEDULE_RATE, powerScheduleService::scheduleFrequentJob), "Thread-ScheduleFrequentJob"));
        // 数据清理
        coreThreadContainer.add(new Thread(new LoopRunnable("CleanWorkerData", PowerScheduleService.SCHEDULE_RATE, powerScheduleService::cleanData), "Thread-CleanWorkerData"));
        // 状态检查
        coreThreadContainer.add(new Thread(new LoopRunnable("CheckRunningInstance", InstanceStatusCheckService.CHECK_INTERVAL, instanceStatusCheckService::checkRunningInstance), "Thread-CheckRunningInstance"));
        coreThreadContainer.add(new Thread(new LoopRunnable("CheckWaitingDispatchInstance", InstanceStatusCheckService.CHECK_INTERVAL, instanceStatusCheckService::checkWaitingDispatchInstance), "Thread-CheckWaitingDispatchInstance"));
        coreThreadContainer.add(new Thread(new LoopRunnable("CheckWaitingWorkerReceiveInstance", InstanceStatusCheckService.CHECK_INTERVAL, instanceStatusCheckService::checkWaitingWorkerReceiveInstance), "Thread-CheckWaitingWorkerReceiveInstance"));
        coreThreadContainer.add(new Thread(new LoopRunnable("CheckWorkflowInstance", InstanceStatusCheckService.CHECK_INTERVAL, instanceStatusCheckService::checkWorkflowInstance), "Thread-CheckWorkflowInstance"));

        coreThreadContainer.forEach(Thread::start);
    }

    @Override
    public void destroy() {
        coreThreadContainer.forEach(Thread::interrupt);
    }


    @RequiredArgsConstructor
    private static class LoopRunnable implements Runnable {

        private final String taskName;

        private final Long runningInterval;

        private final Runnable innerRunnable;

        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            log.info("start task : {}.", taskName);
            while (true) {
                try {
                    innerRunnable.run();
                    Thread.sleep(runningInterval);
                } catch (InterruptedException e) {
                    log.warn("[{}] task has been interrupted!", taskName, e);
                    break;
                } catch (Exception e) {
                    log.error("[{}] task failed!", taskName, e);
                }
            }
        }
    }

}
