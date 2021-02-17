package com.github.kfcfans.powerjob.worker.background;

import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.common.utils.HttpUtils;
import com.github.kfcfans.powerjob.worker.core.tracker.task.TaskTracker;
import com.github.kfcfans.powerjob.worker.core.tracker.task.TaskTrackerPool;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
public class ServerDiscoveryService {

    // 配置的可发起HTTP请求的Server（IP:Port）
    private static final Map<String, String> IP2ADDRESS = Maps.newHashMap();
    // 服务发现地址
    private static final String DISCOVERY_URL = "http://%s/server/acquire?appId=%d&currentServer=%s&protocol=AKKA";
    // 失败次数
    private static int FAILED_COUNT = 0;
    // 最大失败次数
    private static final int MAX_FAILED_COUNT = 3;


    public static String discovery() {

        if (IP2ADDRESS.isEmpty()) {
            OhMyWorker.getConfig().getServerAddress().forEach(x -> IP2ADDRESS.put(x.split(":")[0], x));
        }

        String result = null;

        // 先对当前机器发起请求
        String currentServer = OhMyWorker.getCurrentServer();
        if (!StringUtils.isEmpty(currentServer)) {
            String ip = currentServer.split(":")[0];
            // 直接请求当前Server的HTTP服务，可以少一次网络开销，减轻Server负担
            String firstServerAddress = IP2ADDRESS.get(ip);
            if (firstServerAddress != null) {
                result = acquire(firstServerAddress);
            }
        }

        for (String httpServerAddress : OhMyWorker.getConfig().getServerAddress()) {
            if (StringUtils.isEmpty(result)) {
                result = acquire(httpServerAddress);
            }else {
                break;
            }
        }

        if (StringUtils.isEmpty(result)) {
            log.warn("[OmsServerDiscovery] can't find any available server, this worker has been quarantined.");

            // 在 Server 高可用的前提下，连续失败多次，说明该节点与外界失联，Server已经将秒级任务转移到其他Worker，需要杀死本地的任务
            if (FAILED_COUNT++ > MAX_FAILED_COUNT) {

                log.warn("[OmsServerDiscovery] can't find any available server for 3 consecutive times, It's time to kill all frequent job in this worker.");
                List<Long> frequentInstanceIds = TaskTrackerPool.getAllFrequentTaskTrackerKeys();
                if (!CollectionUtils.isEmpty(frequentInstanceIds)) {
                    frequentInstanceIds.forEach(instanceId -> {
                        TaskTracker taskTracker = TaskTrackerPool.remove(instanceId);
                        taskTracker.destroy();
                        log.warn("[OmsServerDiscovery] kill frequent instance(instanceId={}) due to can't find any available server.", instanceId);
                    });
                }

                FAILED_COUNT = 0;
            }
            return null;
        }else {
            // 重置失败次数
            FAILED_COUNT = 0;
            log.debug("[OmsServerDiscovery] current server is {}.", result);
            return result;
        }
    }

    @SuppressWarnings("rawtypes")
    private static String acquire(String httpServerAddress) {
        String result = null;
        String url = String.format(DISCOVERY_URL, httpServerAddress, OhMyWorker.getAppId(), OhMyWorker.getCurrentServer());
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
