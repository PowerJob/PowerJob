package com.github.kfcfans.powerjob.worker.background;

import akka.actor.ActorSelection;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.PowerJobWorkerVersion;
import com.github.kfcfans.powerjob.worker.common.utils.AkkaUtils;
import com.github.kfcfans.powerjob.worker.common.utils.SystemInfoUtils;
import com.github.kfcfans.powerjob.worker.container.OmsContainerFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Worker健康度定时上报
 *
 * @author tjq
 * @since 2020/3/25
 */
@Slf4j
@AllArgsConstructor
public class WorkerHealthReporter implements Runnable {

    @Override
    public void run() {

        // 没有可用Server，无法上报
        String currentServer = OhMyWorker.getCurrentServer();
        if (StringUtils.isEmpty(currentServer)) {
            return;
        }

        SystemMetrics systemMetrics = SystemInfoUtils.getSystemMetrics();
        WorkerHeartbeat heartbeat = new WorkerHeartbeat();

        heartbeat.setSystemMetrics(systemMetrics);
        heartbeat.setWorkerAddress(OhMyWorker.getWorkerAddress());
        heartbeat.setAppName(OhMyWorker.getConfig().getAppName());
        heartbeat.setAppId(OhMyWorker.getAppId());
        heartbeat.setHeartbeatTime(System.currentTimeMillis());
        heartbeat.setVersion(PowerJobWorkerVersion.getVersion());

        // 获取当前加载的容器列表
        heartbeat.setContainerInfos(OmsContainerFactory.getDeployedContainerInfos());

        // 发送请求
        String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
        if (StringUtils.isEmpty(serverPath)) {
            return;
        }
        ActorSelection actorSelection = OhMyWorker.actorSystem.actorSelection(serverPath);
        actorSelection.tell(heartbeat, null);
    }
}
