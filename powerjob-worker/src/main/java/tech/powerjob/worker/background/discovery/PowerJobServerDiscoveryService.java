package tech.powerjob.worker.background.discovery;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.ImpossibleException;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.WorkerAppInfo;
import tech.powerjob.common.request.ServerDiscoveryRequest;
import tech.powerjob.common.response.ObjectResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.core.tracker.manager.HeavyTaskTrackerManager;
import tech.powerjob.worker.core.tracker.task.heavy.HeavyTaskTracker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务发现
 *
 * @author tjq
 * @since 2023/9/2
 */
@Slf4j
public class PowerJobServerDiscoveryService implements ServerDiscoveryService {

    private final WorkerAppInfo appInfo = new WorkerAppInfo();

    private String currentServerAddress;

    private final Map<String, String> ip2Address = Maps.newHashMap();

    /**
     *  服务发现地址
     */
    private static final String DISCOVERY_URL = "http://%s/server/acquire?%s";

    private static final String ASSERT_URL = "http://%s/server/assert?appName=%s";
    /**
     * 失败次数
     */
    private static int FAILED_COUNT = 0;
    /**
     * 最大失败次数
     */
    private static final int MAX_FAILED_COUNT = 3;

    private final PowerJobWorkerConfig config;

    public PowerJobServerDiscoveryService(PowerJobWorkerConfig config) {
        this.config = config;
    }

    @Override
    public WorkerAppInfo assertApp() {
        try {
            return assertApp0();
        } catch (Exception e) {
            if (config.isAllowLazyConnectServer()) {
                log.warn("[PowerJobWorker] worker is not currently connected to the server, and because allowLazyConnectServer is configured to true it won't block the startup, but you have to be aware that this is dangerous in production environments!");

                // 返回引用，方便后续更新对象内属性
                return appInfo;
            }
            ExceptionUtils.rethrow(e);
        }
        throw new ImpossibleException();
    }

    private WorkerAppInfo assertApp0() {
        String appName = config.getAppName();
        Objects.requireNonNull(appName, "appName can't be empty!");

        for (String server : config.getServerAddress()) {
            String realUrl = String.format(ASSERT_URL, server, appName);
            try {
                String resultDTOStr = CommonUtils.executeWithRetry0(() -> HttpUtils.get(realUrl));
                ObjectResultDTO resultDTO = JsonUtils.parseObject(resultDTOStr, ObjectResultDTO.class);
                if (resultDTO.isSuccess()) {

                    Object resultDataContent = resultDTO.getData();
                    log.info("[PowerJobWorker] assert appName({}) succeed, result from server is: {}.", appName, resultDataContent);
                    // 兼容老版本，响应为数字
                    if (StringUtils.isNumeric(resultDataContent.toString())) {
                        Long appId = Long.valueOf(resultDataContent.toString());
                        this.appInfo.setAppId(appId);
                        return appInfo;
                    }

                    // 新版本，接口直接下发 AppInfo 内容，后续可扩展安全加密等信息
                    WorkerAppInfo serverAppInfo = JsonUtils.parseObject(JsonUtils.toJSONString(resultDataContent), WorkerAppInfo.class);
                    appInfo.setAppId(serverAppInfo.getAppId());
                    return appInfo;
                } else {
                    log.error("[PowerJobWorker] assert appName failed, this appName is invalid, please register the appName {} first.", appName);
                    throw new PowerJobException(resultDTO.getMessage());
                }
            } catch (PowerJobException oe) {
                throw oe;
            } catch (Exception ignore) {
                log.warn("[PowerJobWorker] assert appName by url({}) failed, please check the server address.", realUrl);
            }
        }
        log.error("[PowerJobWorker] no available server in {}.", config.getServerAddress());
        throw new PowerJobException("no server available!");
    }


    @Override
    public String getCurrentServerAddress() {
        return currentServerAddress;
    }

    @Override
    public void timingCheck(ScheduledExecutorService timingPool) {
        this.currentServerAddress = discovery();
        if (StringUtils.isEmpty(this.currentServerAddress) && !config.isAllowLazyConnectServer()) {
            throw new PowerJobException("can't find any available server, this worker has been quarantined.");
        }
        // 这里必须保证成功
        timingPool.scheduleAtFixedRate(() -> {
                    try {
                        this.currentServerAddress = discovery();
                    } catch (Exception e) {
                        log.error("[PowerDiscovery] fail to discovery server!", e);
                    }
                }
                , 10, 10, TimeUnit.SECONDS);
    }

    private String discovery() {

        // 只有允许延迟加载模式下，appId 才可能为空。每次服务发现前，都重新尝试获取 appInfo。由于是懒加载链路，此处完全忽略异常
        if (appInfo.getAppId() == null || appInfo.getAppId() < 0) {
            try {
                assertApp0();
            } catch (Exception e) {
                log.warn("[PowerDiscovery] assertAppName in discovery stage failed, msg: {}", e.getMessage());
                return null;
            }
        }

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
                List<Long> frequentInstanceIds = HeavyTaskTrackerManager.getAllFrequentTaskTrackerKeys();
                if (!CollectionUtils.isEmpty(frequentInstanceIds)) {
                    frequentInstanceIds.forEach(instanceId -> {
                        HeavyTaskTracker taskTracker = HeavyTaskTrackerManager.removeTaskTracker(instanceId);
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


    private String acquire(String httpServerAddress) {
        String result = null;
        String url = buildServerDiscoveryUrl(httpServerAddress);
        try {
            result = CommonUtils.executeWithRetry0(() -> HttpUtils.get(url));
        }catch (Exception ignore) {
        }
        if (!StringUtils.isEmpty(result)) {
            try {
                ObjectResultDTO resultDTO = JsonUtils.parseObject(result, ObjectResultDTO.class);
                if (resultDTO.isSuccess()) {
                    return resultDTO.getData().toString();
                }
            }catch (Exception ignore) {
            }
        }
        return null;
    }

    private String buildServerDiscoveryUrl(String address) {

        ServerDiscoveryRequest serverDiscoveryRequest = new ServerDiscoveryRequest()
                .setAppId(appInfo.getAppId())
                .setCurrentServer(currentServerAddress)
                .setProtocol(config.getProtocol().name().toUpperCase());

        String query = Joiner.on(OmsConstant.AND).withKeyValueSeparator(OmsConstant.EQUAL).join(serverDiscoveryRequest.toMap());
        return String.format(DISCOVERY_URL, address, query);
    }
}
