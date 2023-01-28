package tech.powerjob.remote.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AkkaCSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class AkkaCSInitializer implements CSInitializer {

    private ActorSystem actorSystem;
    private CSInitializerConfig config;

    @Override
    public String type() {
        return tech.powerjob.common.enums.Protocol.AKKA.name();
    }

    @Override
    public void init(CSInitializerConfig config) {

        this.config = config;

        Address bindAddress = config.getBindAddress();
        log.info("[PowerJob-AKKA] bindAddress: {}", bindAddress);

        // 初始化 ActorSystem（macOS上 new ServerSocket 检测端口占用的方法并不生效，可能是AKKA是Scala写的缘故？没办法...只能靠异常重试了）
        Map<String, Object> overrideConfig = Maps.newHashMap();
        overrideConfig.put("akka.remote.artery.canonical.hostname", bindAddress.getHost());
        overrideConfig.put("akka.remote.artery.canonical.port", bindAddress.getPort());

        Config akkaBasicConfig = ConfigFactory.load(AkkaConstant.AKKA_CONFIG);
        Config akkaFinalConfig = ConfigFactory.parseMap(overrideConfig).withFallback(akkaBasicConfig);

        log.info("[PowerJob-AKKA] try to start AKKA System.");

        // 启动时绑定当前的 actorSystemName
        String actorSystemName = AkkaConstant.fetchActorSystemName(config.getServerType());
        this.actorSystem = ActorSystem.create(actorSystemName, akkaFinalConfig);

        // 处理系统中产生的异常情况
        ActorRef troubleshootingActor = actorSystem.actorOf(Props.create(AkkaTroubleshootingActor.class), "troubleshooting");
        actorSystem.eventStream().subscribe(troubleshootingActor, DeadLetter.class);

        log.info("[PowerJob-AKKA] initialize actorSystem[{}] successfully!", actorSystem.name());
    }

    @Override
    public Transporter buildTransporter() {
        return new AkkaTransporter(actorSystem);
    }

    @Override
    public void bindHandlers(List<ActorInfo> actorInfos) {
        int cores = Runtime.getRuntime().availableProcessors();
        actorInfos.forEach(actorInfo -> {
            String rootPath = actorInfo.getAnno().path();
            AkkaMappingService.ActorConfig actorConfig = AkkaMappingService.parseActorName(rootPath);

            log.info("[PowerJob-AKKA] start to process actor[path={},config={}]", rootPath, JsonUtils.toJSONString(actorConfig));

            actorSystem.actorOf(AkkaProxyActor.props(actorInfo)
                    .withDispatcher("akka.".concat(actorConfig.getDispatcherName()))
                    .withRouter(new RoundRobinPool(cores)), actorConfig.getActorName());

        });
    }

    @Override
    public void close() throws IOException {
        actorSystem.terminate();
    }
}
