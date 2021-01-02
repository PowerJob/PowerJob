package com.github.kfcfans.powerjob.worker.background;

import akka.actor.ActorSelection;
import com.github.kfcfans.powerjob.common.LogLevel;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.model.InstanceLogContent;
import com.github.kfcfans.powerjob.common.request.WorkerLogReportReq;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.utils.AkkaUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

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

    private OmsLogHandler() {
    }

    // 单例
    public static final OmsLogHandler INSTANCE = new OmsLogHandler();
    // 生产者消费者模式，异步上传日志
    private final BlockingQueue<InstanceLogContent> logQueue = Queues.newLinkedBlockingQueue();
    // 处理线程，需要通过线程池启动
    public final Runnable logSubmitter = new LogSubmitter();
    // 上报锁，只需要一个线程上报即可
    private final Lock reportLock = new ReentrantLock();

    // 每次上报携带的数据条数
    private static final int BATCH_SIZE = 20;
    // 本地囤积阈值
    private static final int REPORT_SIZE = 1024;

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
        logQueue.offer(tuple);
    }



    private class LogSubmitter implements Runnable {

        @Override
        public void run() {

            boolean lockResult = reportLock.tryLock();
            if (!lockResult) {
                return;
            }

            try {

                String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
                // 当前无可用 Server
                if (StringUtils.isEmpty(serverPath)) {
                    if (!logQueue.isEmpty()) {
                        logQueue.clear();
                        log.warn("[OmsLogHandler] because there is no available server to report logs which leads to queue accumulation, oms discarded all logs.");
                    }
                    return;
                }

                ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);
                List<InstanceLogContent> logs = Lists.newLinkedList();

                while (!logQueue.isEmpty()) {
                    try {
                        InstanceLogContent logContent = logQueue.poll(100, TimeUnit.MILLISECONDS);
                        logs.add(logContent);

                        if (logs.size() >= BATCH_SIZE) {
                            WorkerLogReportReq req = new WorkerLogReportReq(OhMyWorker.getWorkerAddress(), Lists.newLinkedList(logs));
                            // 不可靠请求，WEB日志不追求极致
                            serverActor.tell(req, null);
                            logs.clear();
                        }

                    }catch (Exception ignore) {
                        break;
                    }
                }

                if (!logs.isEmpty()) {
                    WorkerLogReportReq req = new WorkerLogReportReq(OhMyWorker.getWorkerAddress(), logs);
                    serverActor.tell(req, null);
                }

            }finally {
                reportLock.unlock();
            }
        }
    }
}