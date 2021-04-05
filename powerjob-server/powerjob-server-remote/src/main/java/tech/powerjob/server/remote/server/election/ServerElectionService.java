package tech.powerjob.server.remote.server.election;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.alibaba.fastjson.JSONObject;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.transport.TransportService;
import tech.powerjob.server.remote.transport.starter.AkkaStarter;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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

    @Resource
    private LockService lockService;
    @Resource
    private TransportService transportService;
    @Resource
    private AppInfoRepository appInfoRepository;

    @Value("${oms.accurate.select.server.percentage}")
    private int accurateSelectServerPercentage;

    private static final int RETRY_TIMES = 10;
    private static final long PING_TIMEOUT_MS = 1000;
    private static final String SERVER_ELECT_LOCK = "server_elect_%d";

    public String elect(Long appId, String protocol, String currentServer) {
        if (!accurate()) {
            // 如果是本机，就不需要查数据库那么复杂的操作了，直接返回成功
            if (getProtocolServerAddress(protocol).equals(currentServer)) {
                return currentServer;
            }
        }
        return getServer0(appId, protocol);
    }

    private String getServer0(Long appId, String protocol) {

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

                // 篡位，本机作为Server
                // 注意，写入 AppInfoDO#currentServer 的永远是 ActorSystem 的地址，仅在返回的时候特殊处理
                appInfo.setCurrentServer(transportService.getTransporter(Protocol.AKKA).getAddress());
                appInfo.setGmtModified(new Date());

                appInfoRepository.saveAndFlush(appInfo);
                log.info("[ServerElection] this server({}) become the new server for app(appId={}).", appInfo.getCurrentServer(), appId);
                return getProtocolServerAddress(protocol);
            }catch (Exception e) {
                log.error("[ServerElection] write new server to db failed for app {}.", appName, e);
            }finally {
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

        ActorSelection serverActor = AkkaStarter.getFriendActor(serverAddress);
        try {
            CompletionStage<Object> askCS = Patterns.ask(serverActor, ping, Duration.ofMillis(PING_TIMEOUT_MS));
            AskResponse response = (AskResponse) askCS.toCompletableFuture().get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            downServerCache.remove(serverAddress);
            if (response.isSuccess()) {
                return JsonUtils.parseObject(response.getData(), JSONObject.class).getString(protocol);
            }
        }catch (Exception e) {
            log.warn("[ServerElection] server({}) was down.", serverAddress);
        }
        downServerCache.add(serverAddress);
        return null;
    }

    private boolean accurate() {
        return ThreadLocalRandom.current().nextInt(100) < accurateSelectServerPercentage;
    }

    private String getProtocolServerAddress(String protocol) {
        Protocol pt = Protocol.of(protocol);
        return TransportService.getAllAddress().get(pt);
    }
}
