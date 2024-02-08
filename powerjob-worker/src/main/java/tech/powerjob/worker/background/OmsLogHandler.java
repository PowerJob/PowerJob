package tech.powerjob.worker.background;

import tech.powerjob.common.enhance.SafeRunnable;
import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.model.InstanceLogContent;
import tech.powerjob.common.request.WorkerLogReportReq;
import tech.powerjob.remote.framework.transporter.Transporter;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.worker.background.discovery.ServerDiscoveryService;
import tech.powerjob.worker.common.utils.TransportUtils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志处理器
 *
 * @author tjq
 * @since 2020/4/21
 */
@Slf4j
public class OmsLogHandler {

    private final String workerAddress;
    private final Transporter transporter;
    private final ServerDiscoveryService serverDiscoveryService;

    // 处理线程，需要通过线程池启动
    public final Runnable logSubmitter = new LogSubmitter();
    // 上报锁，只需要一个线程上报即可
    private final Lock reportLock = new ReentrantLock();
    // 生产者消费者模式，异步上传日志
    private final BlockingQueue<InstanceLogContent> logQueue = Queues.newLinkedBlockingQueue(10240);

    // 每次上报携带的数据条数
    private static final int BATCH_SIZE = 20;
    // 本地囤积阈值
    private static final int REPORT_SIZE = 1024;

    public OmsLogHandler(String workerAddress, Transporter transporter, ServerDiscoveryService serverDiscoveryService) {
        this.workerAddress = workerAddress;
        this.transporter = transporter;
        this.serverDiscoveryService = serverDiscoveryService;
    }

    /**
     * 提交日志
     * @param instanceId 任务实例ID
     * @param logContent 日志内容
     */
    public void submitLog(long instanceId, LogLevel logLevel, String logContent) {

        if (logQueue.size() > REPORT_SIZE) {
            // 线程的生命周期是个不可循环的过程，一个线程对象结束了不能再次start，只能一直创建和销毁
            new Thread(logSubmitter).start();
        }

        InstanceLogContent tuple = new InstanceLogContent(instanceId, System.currentTimeMillis(), logLevel.getV(), logContent);
        boolean offerRet = logQueue.offer(tuple);
        if (!offerRet) {
            log.warn("[OmsLogHandler] [{}] submit log failed, maybe your log speed is too fast!", instanceId);
        }
    }



    private class LogSubmitter extends SafeRunnable {

        @Override
        public void run0() {

            boolean lockResult = reportLock.tryLock();
            if (!lockResult) {
                return;
            }

            try {

                final String currentServerAddress = serverDiscoveryService.getCurrentServerAddress();
                // 当前无可用 Server
                if (StringUtils.isEmpty(currentServerAddress)) {
                    if (!logQueue.isEmpty()) {
                        logQueue.clear();
                        log.warn("[OmsLogHandler] because there is no available server to report logs which leads to queue accumulation, oms discarded all logs.");
                    }
                    return;
                }

                List<InstanceLogContent> logs = Lists.newLinkedList();

                while (!logQueue.isEmpty()) {
                    try {
                        InstanceLogContent logContent = logQueue.poll(100, TimeUnit.MILLISECONDS);
                        logs.add(logContent);

                        if (logs.size() >= BATCH_SIZE) {
                            WorkerLogReportReq req = new WorkerLogReportReq(workerAddress, Lists.newLinkedList(logs));
                            // 不可靠请求，WEB日志不追求极致
                            TransportUtils.reportLogs(req, currentServerAddress, transporter);
                            logs.clear();
                        }

                    }catch (Exception ignore) {
                        break;
                    }
                }

                if (!logs.isEmpty()) {
                    WorkerLogReportReq req = new WorkerLogReportReq(workerAddress, logs);
                    TransportUtils.reportLogs(req, currentServerAddress, transporter);
                }

            }finally {
                reportLock.unlock();
            }
        }
    }
}