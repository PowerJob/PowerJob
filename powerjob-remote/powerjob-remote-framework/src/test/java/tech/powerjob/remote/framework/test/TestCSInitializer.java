package tech.powerjob.remote.framework.test;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.IOException;
import java.util.List;

/**
 * TestCSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
public class TestCSInitializer implements CSInitializer {
    @Override
    public String type() {
        return "TEST";
    }

    @Override
    public void init(CSInitializerConfig config) {
        log.info("TestCSInitializer#init");
    }

    @Override
    public Transporter buildTransporter() {
        log.info("TestCSInitializer#buildTransporter");
        return null;
    }

    @Override
    public void bindHandlers(List<ActorInfo> actorInfos) {
        log.info("TestCSInitializer#actorInfos");
    }

    @Override
    public void close() throws IOException {
        log.info("TestCSInitializer#close");
    }
}
