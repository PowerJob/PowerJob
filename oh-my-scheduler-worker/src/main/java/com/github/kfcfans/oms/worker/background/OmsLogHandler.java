package com.github.kfcfans.oms.worker.background;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.model.InstanceLogContent;
import com.github.kfcfans.common.request.WorkerLogReportReq;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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


    private static final int BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 8096;

    /**
     * 提交日志
     * @param instanceId 任务实例ID
     * @param logContent 日志内容
     */
    public void submitLog(long instanceId, String logContent) {
        InstanceLogContent tuple = new InstanceLogContent(instanceId, System.currentTimeMillis(), logContent);
        logQueue.add(tuple);
    }

    private class LogSubmitter implements Runnable {

        @Override
        public void run() {

            String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
            // 当前无可用 Server
            if (StringUtils.isEmpty(serverPath)) {

                // 防止长时间无可用Server导致的堆积
                if (logQueue.size() > MAX_QUEUE_SIZE) {
                    for (int i = 0; i < 1024; i++) {
                        logQueue.remove();
                    }
                    log.warn("[OmsLogHandler] because there is no available server to report logs which leads to queue accumulation, oms discarded some logs.");
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
                        WorkerLogReportReq req = new WorkerLogReportReq(OhMyWorker.getWorkerAddress(), logs);
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
        }
    }
}