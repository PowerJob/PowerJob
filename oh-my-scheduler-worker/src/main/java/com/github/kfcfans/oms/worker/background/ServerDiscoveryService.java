package com.github.kfcfans.oms.worker.background;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
public class ServerDiscoveryService {

    private static final String DISCOVERY_URL = "http://%s/server/acquire?appId=%d&currentServer=%s";

    @SuppressWarnings("rawtypes")
    public static String discovery() {

        String result = null;
        for (String httpServerAddress : OhMyWorker.getConfig().getServerAddress()) {

            String url = String.format(DISCOVERY_URL, httpServerAddress, OhMyWorker.getAppId(), OhMyWorker.getCurrentServer());
            try {
                result = CommonUtils.executeWithRetry0(() -> HttpUtils.get(url));
            }catch (Exception ignore) {
            }

            if (!StringUtils.isEmpty(result)) {

                ResultDTO resultDTO = JSONObject.parseObject(result, ResultDTO.class);
                if (resultDTO.isSuccess()) {
                    String server = resultDTO.getData().toString();
                    log.debug("[OMS-ServerDiscoveryService] current server is {}.", server);
                    return server;
                }
            }
        }
        log.error("[OMS-ServerDiscoveryService] can't find any available server, this worker has been quarantined.");
        return null;
    }

}
