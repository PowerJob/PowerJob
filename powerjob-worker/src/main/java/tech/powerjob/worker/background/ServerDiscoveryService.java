package tech.powerjob.worker.background;

import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.core.tracker.task.TaskTracker;
import tech.powerjob.worker.core.tracker.task.TaskTrackerPool;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
public class ServerDiscoveryService {

    private final Long appId;
    private final PowerJobWorkerConfig config;

    private String currentServerAddress;

    private final Map<String, String> ip2Address = Maps.newHashMap();

    // 服务发现地址
    private static final String DISCOVERY_URL = "http://%s/server/acquire?appId=%d&currentServer=%s&protocol=AKKA";
    // 失败次数
    private static int FAILED_COUNT = 0;
    // 最大失败次数
    private static final int MAX_FAILED_COUNT = 3;

    public ServerDiscoveryService(Long appId, PowerJobWorkerConfig config) {
        this.appId = appId;
        this.config = config;
    }

    public void start(ScheduledExecutorService timingPool) {
        this.currentServerAddress = discovery();
        if (org.springframework.util.StringUtils.isEmpty(this.currentServerAddress) && !config.isEnableTestMode()) {
            throw new PowerJobException("can't find any available server, this worker has been quarantined.");
        }
        timingPool.scheduleAtFixedRate(() -> this.currentServerAddress = discovery(), 10, 10, TimeUnit.SECONDS);
    }

    public String getCurrentServerAddress() {
        return currentServerAddress;
    }


    private String discovery() {

        if (ip2Address.isEmpty()) {
            config.getServerAddress().forEach(x -> ip2Address.put(x.split(":")[0], x));
        }

        String result = null;

        // 先对当前机器发起请求
        String currentServer = currentServerAddress;
        if (!StringUtils.isEmpty(currentServer)) {
            String ip = currentServer.split(":")[0];
            // 直接请求当前Server的HTTP服务，可以少一次网络开销，减轻Server负担
            String firstServerAddress = ip2Address.get(ip);
            if (firstServerAddress != null) {
                result = acquire(firstServerAddress);
            }
        }

        for (String httpServerAddress : config.getServerAddress()) {
            if (StringUtils.isEmpty(result)) {
                result = acquire(httpServerAddress);
            }else {
                break;
            }
        }

        if (StringUtils.isEmpty(result)) {
            log.warn("[PowerDiscovery] can't find any available server, this worker has been quarantined.");

            // 在 Server 高可用的前提下，连续失败多次，说明该节点与外界失联，Server已经将秒级任务转移到其他Worker，需要杀死本地的任务
            if (FAILED_COUNT++ > MAX_FAILED_COUNT) {

                log.warn("[PowerDiscovery] can't find any available server for 3 consecutive times, It's time to kill all frequent job in this worker.");
                List<Long> frequentInstanceIds = TaskTrackerPool.getAllFrequentTaskTrackerKeys();
                if (!CollectionUtils.isEmpty(frequentInstanceIds)) {
                    frequentInstanceIds.forEach(instanceId -> {
                        TaskTracker taskTracker = TaskTrackerPool.remove(instanceId);
                        taskTracker.destroy();
                        log.warn("[PowerDiscovery] kill frequent instance(instanceId={}) due to can't find any available server.", instanceId);
                    });
                }

                FAILED_COUNT = 0;
            }
            return null;
        } else {
            // 重置失败次数
            FAILED_COUNT = 0;
            log.debug("[PowerDiscovery] current server is {}.", result);
            return result;
        }
    }

    @SuppressWarnings("rawtypes")
    private String acquire(String httpServerAddress) {
        String result = null;
        String url = String.format(DISCOVERY_URL, httpServerAddress, appId, currentServerAddress);
        try {
            result = CommonUtils.executeWithRetry0(() -> HttpUtils.get(url));
        }catch (Exception ignore) {
        }
        if (!StringUtils.isEmpty(result)) {
            try {
                ResultDTO resultDTO = JsonUtils.parseObject(result, ResultDTO.class);
                if (resultDTO.isSuccess()) {
                    return resultDTO.getData().toString();
                }
            }catch (Exception ignore) {
            }
        }
        return null;
    }
}
