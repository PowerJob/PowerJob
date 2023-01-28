package tech.powerjob.server.remote.server.election;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.request.ServerDiscoveryRequest;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.transporter.ProtocolInfo;
import tech.powerjob.server.remote.transporter.impl.ServerURLFactory;
import tech.powerjob.server.remote.transporter.TransportService;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Default server election policy, first-come, first-served, no load balancing capability
 *
 * @author tjq
 * @since 2021/2/9
 */
@Slf4j
@Service
public class ServerElectionService {

    private final LockService lockService;

    private final TransportService transportService;

    private final AppInfoRepository appInfoRepository;

    private final int accurateSelectServerPercentage;

    private static final int RETRY_TIMES = 10;
    private static final long PING_TIMEOUT_MS = 1000;
    private static final String SERVER_ELECT_LOCK = "server_elect_%d";

    public ServerElectionService(LockService lockService, TransportService transportService, AppInfoRepository appInfoRepository,@Value("${oms.accurate.select.server.percentage}") int accurateSelectServerPercentage) {
        this.lockService = lockService;
        this.transportService = transportService;
        this.appInfoRepository = appInfoRepository;
        this.accurateSelectServerPercentage = accurateSelectServerPercentage;
    }

    public String elect(ServerDiscoveryRequest request) {
        if (!accurate()) {
            final String currentServer = request.getCurrentServer();
            // 如果是本机，就不需要查数据库那么复杂的操作了，直接返回成功
            Optional<ProtocolInfo> localProtocolInfoOpt = Optional.ofNullable(transportService.allProtocols().get(request.getProtocol()));
            if (localProtocolInfoOpt.isPresent() && localProtocolInfoOpt.get().getAddress().equals(currentServer)) {
                log.debug("[ServerElectionService] this server[{}] is worker's current server, skip check", currentServer);
                return currentServer;
            }
        }
        return getServer0(request);
    }

    private String getServer0(ServerDiscoveryRequest discoveryRequest) {

        final Long appId = discoveryRequest.getAppId();
        final String protocol = discoveryRequest.getProtocol();
        Set<String> downServerCache = Sets.newHashSet();

        for (int i = 0; i < RETRY_TIMES; i++) {

            // 无锁获取当前数据库中的Server
            Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
            if (!appInfoOpt.isPresent()) {
                throw new PowerJobException(appId + " is not registered!");
            }
            String appName = appInfoOpt.get().getAppName();
            String originServer = appInfoOpt.get().getCurrentServer();
            String activeAddress = activeAddress(originServer, downServerCache, protocol);
            if (StringUtils.isNotEmpty(activeAddress)) {
                return activeAddress;
            }

            // 无可用Server，重新进行Server选举，需要加锁
            String lockName = String.format(SERVER_ELECT_LOCK, appId);
            boolean lockStatus = lockService.tryLock(lockName, 30000);
            if (!lockStatus) {
                try {
                    Thread.sleep(500);
                }catch (Exception ignore) {
                }
                continue;
            }
            try {

                // 可能上一台机器已经完成了Server选举，需要再次判断
                AppInfoDO appInfo = appInfoRepository.findById(appId).orElseThrow(() -> new RuntimeException("impossible, unless we just lost our database."));
                String address = activeAddress(appInfo.getCurrentServer(), downServerCache, protocol);
                if (StringUtils.isNotEmpty(address)) {
                    return address;
                }

                // 篡位，如果本机存在协议，则作为Server调度该 worker
                final ProtocolInfo targetProtocolInfo = transportService.allProtocols().get(protocol);
                if (targetProtocolInfo != null) {
                    // 注意，写入 AppInfoDO#currentServer 的永远是 default 的地址，仅在返回的时候特殊处理为协议地址
                    appInfo.setCurrentServer(transportService.defaultProtocol().getAddress());
                    appInfo.setGmtModified(new Date());

                    appInfoRepository.saveAndFlush(appInfo);
                    log.info("[ServerElection] this server({}) become the new server for app(appId={}).", appInfo.getCurrentServer(), appId);
                    return targetProtocolInfo.getAddress();
                }
            }catch (Exception e) {
                log.error("[ServerElection] write new server to db failed for app {}.", appName, e);
            } finally {
                lockService.unlock(lockName);
            }
        }
        throw new PowerJobException("server elect failed for app " + appId);
    }

    /**
     * 判断指定server是否存活
     * @param serverAddress 需要检测的server地址
     * @param downServerCache 缓存，防止多次发送PING（这个QPS其实还蛮爆表的...）
     * @param protocol 协议，用于返回指定的地址
     * @return null or address
     */
    private String activeAddress(String serverAddress, Set<String> downServerCache, String protocol) {

        if (downServerCache.contains(serverAddress)) {
            return null;
        }
        if (StringUtils.isEmpty(serverAddress)) {
            return null;
        }

        Ping ping = new Ping();
        ping.setCurrentTime(System.currentTimeMillis());

        URL targetUrl = ServerURLFactory.ping2Friend(serverAddress);
        try {
            AskResponse response = transportService.ask(Protocol.HTTP.name(), targetUrl, ping, AskResponse.class)
                    .toCompletableFuture()
                    .get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response.isSuccess()) {
                // 检测通过的是远程 server 的暴露地址，需要返回 worker 需要的协议地址
                final JSONObject protocolInfo = JsonUtils.parseObject(response.getData(), JSONObject.class).getJSONObject(protocol);
                if (protocolInfo != null) {
                    downServerCache.remove(serverAddress);
                    final String protocolAddress = protocolInfo.toJavaObject(ProtocolInfo.class).getAddress();
                    log.info("[ServerElection] server[{}] is active, it will be the master, final protocol address={}", serverAddress, protocolAddress);
                    return protocolAddress;
                } else {
                    log.warn("[ServerElection] server[{}] is active but don't have target protocol", serverAddress);
                }
            }
        }catch (Exception e) {
            log.warn("[ServerElection] server[{}] was down.", serverAddress, e);
        }
        downServerCache.add(serverAddress);
        return null;
    }

    private boolean accurate() {
        return ThreadLocalRandom.current().nextInt(100) < accurateSelectServerPercentage;
    }
}
