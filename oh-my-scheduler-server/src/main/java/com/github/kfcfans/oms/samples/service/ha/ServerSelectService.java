package com.github.kfcfans.oms.samples.service.ha;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.oms.common.response.AskResponse;
import com.github.kfcfans.oms.samples.akka.OhMyServer;
import com.github.kfcfans.oms.samples.akka.requests.Ping;
import com.github.kfcfans.oms.samples.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.samples.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.samples.service.lock.LockService;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Worker请求分配Server服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class ServerSelectService {

    @Resource
    private LockService lockService;
    @Resource
    private AppInfoRepository appInfoRepository;

    private static final int RETRY_TIMES = 10;
    private static final long PING_TIMEOUT_MS = 1000;
    private static final String SERVER_ELECT_LOCK = "server_elect_%d";

    /**
     * 获取某个应用对应的Server
     * 缺点：如果server死而复生，可能造成worker集群脑裂，不过感觉影响不是很大 & 概率极低，就不管了
     * @param appId 应用ID
     * @return 当前可用的Server
     */
    public String getServer(Long appId) {

        Set<String> downServerCache = Sets.newHashSet();

        for (int i = 0; i < RETRY_TIMES; i++) {

            // 无锁获取当前数据库中的Server
            Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
            if (!appInfoOpt.isPresent()) {
                throw new RuntimeException(appId + " is not registered!");
            }
            String appName = appInfoOpt.get().getAppName();
            String originServer = appInfoOpt.get().getCurrentServer();
            if (isActive(originServer, downServerCache)) {
                return originServer;
            }

            // 无可用Server，重新进行Server选举，需要加锁
            String lockName = String.format(SERVER_ELECT_LOCK, appId);
            boolean lockStatus = lockService.lock(lockName);
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
                if (isActive(appInfo.getCurrentServer(), downServerCache)) {
                    return appInfo.getCurrentServer();
                }

                // 篡位，本机作为Server
                appInfo.setCurrentServer(OhMyServer.getActorSystemAddress());
                appInfo.setGmtModified(new Date());

                appInfoRepository.saveAndFlush(appInfo);
                log.info("[ServerSelectService] this server({}) become the new server for app(appId={}).", appInfo.getCurrentServer(), appId);
                return appInfo.getCurrentServer();
            }catch (Exception e) {
                log.warn("[ServerSelectService] write new server to db failed for app {}.", appName);
            }finally {
                lockService.unlock(lockName);
            }
        }
        throw new RuntimeException("server elect failed for app " + appId);
    }

    /**
     * 判断指定server是否存活
     * @param serverAddress 需要检测的server地址
     * @param downServerCache 缓存，防止多次发送PING（这个QPS其实还蛮爆表的...）
     * @return true -> 存活 / false -> down机
     */
    private boolean isActive(String serverAddress, Set<String> downServerCache) {

        if (downServerCache.contains(serverAddress)) {
            return false;
        }
        if (StringUtils.isEmpty(serverAddress)) {
            return false;
        }

        Ping ping = new Ping();
        ping.setCurrentTime(System.currentTimeMillis());

        ActorSelection serverActor = OhMyServer.getFriendActor(serverAddress);
        try {
            CompletionStage<Object> askCS = Patterns.ask(serverActor, ping, Duration.ofMillis(PING_TIMEOUT_MS));
            AskResponse response = (AskResponse) askCS.toCompletableFuture().get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            downServerCache.remove(serverAddress);
            return response.isSuccess();
        }catch (Exception e) {
            log.warn("[ServerSelectService] server({}) was down.", serverAddress);
        }
        downServerCache.add(serverAddress);
        return false;
    }
}
