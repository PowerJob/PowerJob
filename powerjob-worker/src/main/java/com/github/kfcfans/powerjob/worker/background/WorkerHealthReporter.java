package com.github.kfcfans.powerjob.worker.background;

import akka.actor.ActorSelection;
import com.github.kfcfans.powerjob.common.Protocol;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.github.kfcfans.powerjob.worker.common.PowerJobWorkerVersion;
import com.github.kfcfans.powerjob.worker.common.WorkerRuntime;
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

    private final WorkerRuntime workerRuntime;

    @Override
    public void run() {

        // 没有可用Server，无法上报
        String currentServer = workerRuntime.getServerDiscoveryService().getCurrentServerAddress();
        if (StringUtils.isEmpty(currentServer)) {
            return;
        }

        SystemMetrics systemMetrics;

        if (workerRuntime.getOhMyConfig().getSystemMetricsCollector() == null) {
            systemMetrics = SystemInfoUtils.getSystemMetrics();
        } else {
            systemMetrics = workerRuntime.getOhMyConfig().getSystemMetricsCollector().collect();
        }

        WorkerHeartbeat heartbeat = new WorkerHeartbeat();

        heartbeat.setSystemMetrics(systemMetrics);
        heartbeat.setWorkerAddress(workerRuntime.getWorkerAddress());
        heartbeat.setAppName(workerRuntime.getOhMyConfig().getAppName());
        heartbeat.setAppId(workerRuntime.getAppId());
        heartbeat.setHeartbeatTime(System.currentTimeMillis());
        heartbeat.setVersion(PowerJobWorkerVersion.getVersion());
        heartbeat.setProtocol(Protocol.AKKA.name());
        heartbeat.setClient("Atlantis");
        heartbeat.setTag(workerRuntime.getOhMyConfig().getTag());

        // 获取当前加载的容器列表
        heartbeat.setContainerInfos(OmsContainerFactory.getDeployedContainerInfos());

        // 发送请求
        String serverPath = AkkaUtils.getServerActorPath(currentServer);
        if (StringUtils.isEmpty(serverPath)) {
            return;
        }
        ActorSelection actorSelection = workerRuntime.getActorSystem().actorSelection(serverPath);
        actorSelection.tell(heartbeat, null);
    }
}
