package com.github.kfcfans.oms.worker.background;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.utils.HttpUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
public class ServerDiscoveryService {

    private static final Map<String, String> IP2ADDRESS = Maps.newHashMap();
    private static final String DISCOVERY_URL = "http://%s/server/acquire?appId=%d&currentServer=%s";


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
            result = acquire(firstServerAddress);
        }

        for (String httpServerAddress : OhMyWorker.getConfig().getServerAddress()) {
            if (StringUtils.isEmpty(result)) {
                result = acquire(httpServerAddress);
            }else {
                break;
            }
        }

        if (StringUtils.isEmpty(result)) {
            log.error("[OMS-ServerDiscoveryService] can't find any available server, this worker has been quarantined.");
            return null;
        }else {
            log.debug("[OMS-ServerDiscoveryService] current server is {}.", result);
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
            ResultDTO resultDTO = JSONObject.parseObject(result, ResultDTO.class);
            if (resultDTO.isSuccess()) {
                return resultDTO.getData().toString();
            }
        }
        return null;
    }
}
