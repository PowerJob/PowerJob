package com.github.kfcfans.oms.worker;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.kfcfans.oms.worker.actors.ProcessorTrackerActor;
import com.github.kfcfans.oms.worker.actors.TaskTrackerActor;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.oms.worker.common.constants.AkkaConstant;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.common.utils.SpringUtils;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 客户端启动类
 *
 * @author KFCFans
 * @since 2020/3/16
 */
@Slf4j
public class OhMyWorker implements ApplicationContextAware, InitializingBean {

    private static OhMyConfig config;
    public static ActorSystem actorSystem;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.inject(applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("[OhMyWorker] start to initialize OhMyWorker...");

        try {

            // 初始化 ActorSystem
            Map<String, Object> overrideConfig = Maps.newHashMap();
            String localIP = StringUtils.isEmpty(config.getListeningIP()) ? NetUtils.getLocalHost() : config.getListeningIP();
            overrideConfig.put("akka.remote.artery.canonical.hostname", localIP);
            if (config.getListeningPort() != null) {
                overrideConfig.put("akka.remote.artery.canonical.port", config.getListeningPort());
            }
            log.info("[OhMyWorker] akka-remote listening address config: {}", overrideConfig);
            Config akkaBasicConfig = ConfigFactory.load(AkkaConstant.AKKA_CONFIG_NAME);
            Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

            actorSystem = ActorSystem.create(AkkaConstant.ACTOR_SYSTEM_NAME, akkaFinalConfig);
            actorSystem.actorOf(Props.create(TaskTrackerActor.class));
            actorSystem.actorOf(Props.create(ProcessorTrackerActor.class));
            log.info("[OhMyWorker] akka ActorSystem({}) initialized successfully.", actorSystem);

            // 初始化存储
            TaskPersistenceService.INSTANCE.init();
            log.info("[OhMyWorker] local storage initialized successfully.");


            log.info("[OhMyWorker] OhMyWorker initialized successfully, using time: {}, congratulations!", stopwatch);
        }catch (Exception e) {
            log.error("[OhMyWorker] initialize OhMyWorker failed, using {}.", stopwatch, e);
        }

    }

    public static OhMyConfig getConfig() {
        return config;
    }
    public void setConfig(OhMyConfig cfg) {
        config = cfg;
    }

    public static void main(String[] args) {

        System.out.println(org.h2.util.NetUtils.getLocalAddress());

        OhMyConfig config = new OhMyConfig();
        config.setAppName("oms");
        OhMyWorker ohMyWorker = new OhMyWorker();
        ohMyWorker.setConfig(config);
        ohMyWorker.init();


    }
}
