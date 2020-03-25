package com.github.kfcfans.oms.worker.background;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.AkkaConstant;
import com.github.kfcfans.common.model.SystemMetrics;
import com.github.kfcfans.common.request.WorkerHealthReportReq;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.SystemInfoUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Worker健康度定时上报
 *
 * @author tjq
 * @since 2020/3/25
 */
@Slf4j
@AllArgsConstructor
public class WorkerHealthReportRunnable implements Runnable {

    @Override
    public void run() {

        SystemMetrics systemMetrics = SystemInfoUtils.getSystemMetrics();

        WorkerHealthReportReq reportReq = new WorkerHealthReportReq();
        reportReq.setSystemMetrics(systemMetrics);
        reportReq.setTotalAddress(OhMyWorker.getWorkerAddress());

        // 发送请求
        String serverPath = AkkaUtils.getAkkaServerNodePath(AkkaConstant.SERVER_ACTOR_NAME);
        ActorSelection actorSelection = OhMyWorker.actorSystem.actorSelection(serverPath);
        actorSelection.tell(reportReq, null);
    }
}
