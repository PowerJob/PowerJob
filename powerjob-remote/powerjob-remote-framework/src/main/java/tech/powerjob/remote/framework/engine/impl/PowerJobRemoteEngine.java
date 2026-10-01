package tech.powerjob.remote.framework.engine.impl;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.utils.SysUtils;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.TestActor;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.IOException;
import java.util.List;

/**
 * 初始化 PowerJob 整个网络层
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class PowerJobRemoteEngine implements RemoteEngine {

    private CSInitializer csInitializer;

    @Override
    public EngineOutput start(EngineConfig engineConfig) {

        reConfig(engineConfig);

        final String engineType = engineConfig.getType();
        EngineOutput engineOutput = new EngineOutput();
        log.info("[PowerJobRemoteEngine] [{}] start remote engine with config: {}", engineType, engineConfig);

        List<ActorInfo> actorInfos = ActorFactory.load(engineConfig.getActorList());
        csInitializer = CSInitializerFactory.build(engineType);

        String type = csInitializer.type();

        Stopwatch sw = Stopwatch.createStarted();
        log.info("[PowerJobRemoteEngine] [{}] try to startup CSInitializer[type={}]", engineType, type);

        csInitializer.init(new CSInitializerConfig()
                .setBindAddress(engineConfig.getBindAddress())
                .setExternalAddress(engineConfig.getExternalAddress())
                .setServerType(engineConfig.getServerType())
        );

        // 构建通讯器
        Transporter transporter = csInitializer.buildTransporter();
        engineOutput.setTransporter(transporter);

        log.info("[PowerJobRemoteEngine] [{}] start to bind Handler", engineType);
        actorInfos.forEach(actor -> actor.getHandlerInfos().forEach(handlerInfo -> log.info("[PowerJobRemoteEngine] [{}] PATH={}, handler={}", engineType, handlerInfo.getLocation().toPath(), handlerInfo.getMethod())));

        // 绑定 handler
        csInitializer.bindHandlers(actorInfos);

        log.info("[PowerJobRemoteEngine] [{}] startup successfully, cost: {}", engineType, sw);

        return engineOutput;
    }

    @Override
    public void close() throws IOException {
        if (csInitializer != null) {
            csInitializer.close();
        }
    }

    private static void reConfig(EngineConfig engineConfig) {
        boolean testEnv = SysUtils.isTestEnv();
        if (testEnv) {
            log.info("[PowerJobRemoteEngine] add 'TestActor' due to current is test env");
            engineConfig.getActorList().add(new TestActor());
        }
    }
}
