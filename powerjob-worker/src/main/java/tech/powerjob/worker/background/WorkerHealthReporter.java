package tech.powerjob.worker.background;

import akka.actor.ActorSelection;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.common.request.WorkerHeartbeat;
import tech.powerjob.worker.common.PowerJobWorkerVersion;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.common.utils.SystemInfoUtils;
import tech.powerjob.worker.container.OmsContainerFactory;
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

        if (workerRuntime.getWorkerConfig().getSystemMetricsCollector() == null) {
            systemMetrics = SystemInfoUtils.getSystemMetrics();
        } else {
            systemMetrics = workerRuntime.getWorkerConfig().getSystemMetricsCollector().collect();
        }

        WorkerHeartbeat heartbeat = new WorkerHeartbeat();

        heartbeat.setSystemMetrics(systemMetrics);
        heartbeat.setWorkerAddress(workerRuntime.getWorkerAddress());
        heartbeat.setAppName(workerRuntime.getWorkerConfig().getAppName());
        heartbeat.setAppId(workerRuntime.getAppId());
        heartbeat.setHeartbeatTime(System.currentTimeMillis());
        heartbeat.setVersion(PowerJobWorkerVersion.getVersion());
        heartbeat.setProtocol(Protocol.AKKA.name());
        heartbeat.setClient("Atlantis");
        heartbeat.setTag(workerRuntime.getWorkerConfig().getTag());

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
