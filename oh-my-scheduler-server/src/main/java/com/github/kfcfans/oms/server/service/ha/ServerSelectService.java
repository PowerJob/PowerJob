package com.github.kfcfans.oms.server.service.ha;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.server.core.akka.OhMyServer;
import com.github.kfcfans.oms.server.core.akka.Ping;
import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.service.lock.LockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
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
    private static final long PING_TIMEOUT_MS = 5000;
    private static final long WAIT_LOCK_TIME = 1000;
    private static final String SERVER_ELECT_LOCK = "server_elect_%s";

    /**
     * 获取某个应用对应的Server
     * 缺点：如果server死而复生，可能造成worker集群脑裂，不过感觉影响不是很大 & 概率极低，就不管了
     * @param appName 应用名称
     * @return 当前可用的Server
     */
    public String getServer(String appName) {

        for (int i = 0; i < RETRY_TIMES; i++) {

            // 无锁获取当前数据库中的Server
            AppInfoDO app = appInfoRepository.findByAppName(appName);
            if (app == null) {
                throw new RuntimeException(appName + " is not registered!");
            }
            String originServer = app.getCurrentServer();
            if (isActive(originServer)) {
                return originServer;
            }

            // 获取失败，重新进行Server选举，需要加锁
            String lockName = String.format(SERVER_ELECT_LOCK, appName);
            boolean lockStatus = lockService.lock(lockName);
            if (!lockStatus) {
                try {
                    Thread.sleep(1000);
                }catch (Exception ignore) {
                }
                continue;
            }
            try {

                // 可能上一台机器已经完成了Server选举，需要再次判断
                AppInfoDO appInfo = appInfoRepository.findByAppName(appName);
                if (isActive(appInfo.getCurrentServer())) {
                    return appInfo.getCurrentServer();
                }

                // 篡位，本机作为Server
                appInfo.setCurrentServer(OhMyServer.getActorSystemAddress());
                appInfo.setGmtModified(new Date());

                appInfoRepository.saveAndFlush(appInfo);
                return appInfo.getCurrentServer();
            }catch (Exception e) {
                log.warn("[ServerSelectService] write new server to db failed for app {}.", appName);
            }finally {
                lockService.unlock(lockName);
            }
        }
        throw new RuntimeException("server elect failed for app " + appName);
    }

    private boolean isActive(String serverAddress) {
        if (StringUtils.isEmpty(serverAddress)) {
            return false;
        }
        Ping ping = new Ping();
        ping.setCurrentTime(System.currentTimeMillis());

        ActorSelection serverActor = OhMyServer.getServerActor(serverAddress);
        try {
            CompletionStage<Object> askCS = Patterns.ask(serverActor, ping, Duration.ofMillis(PING_TIMEOUT_MS));
            AskResponse response = (AskResponse) askCS.toCompletableFuture().get(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return response.isSuccess();
        }catch (Exception e) {
            log.warn("[ServerSelectService] server({}) was down, try to elect a new server.", serverAddress);
        }
        return false;
    }
}
