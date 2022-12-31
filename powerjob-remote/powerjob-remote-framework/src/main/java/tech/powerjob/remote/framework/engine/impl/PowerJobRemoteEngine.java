package tech.powerjob.remote.framework.engine.impl;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.List;

/**
 * 初始化 PowerJob 整个网络层
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class PowerJobRemoteEngine implements RemoteEngine {

    @Override
    public EngineOutput start(EngineConfig engineConfig) {

        EngineOutput engineOutput = new EngineOutput();
        log.info("[PowerJobRemoteEngine] start remote engine with config: {}", engineConfig);

        List<HandlerInfo> handlerInfos = HandlerFactory.load();
        List<CSInitializer> csInitializerList = CSInitializerFactory.build(engineConfig.getTypes());

        csInitializerList.forEach(csInitializer -> {

            String type = csInitializer.type();

            Stopwatch sw = Stopwatch.createStarted();
            log.info("[PowerJobRemoteEngine] try to startup CSInitializer[type={}]", type);

            csInitializer.init(new CSInitializerConfig()
                    .setBindAddress(engineConfig.getBindAddress())
                    .setServerType(engineConfig.getServerType())
            );
            Transporter transporter = csInitializer.buildTransporter();
            engineOutput.getType2Transport().put(type, transporter);

            csInitializer.bindHandlers(handlerInfos);

            log.info("[PowerJobRemoteEngine] startup CSInitializer[type={}] successfully, cost: {}", type, sw);
        });

        return engineOutput;
    }
}
