package tech.powerjob.remote.http;

import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.IOException;
import java.util.List;

/**
 * HttpCSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
public class HttpCSInitializer implements CSInitializer {

    @Override
    public String type() {
        return null;
    }

    @Override
    public void init(CSInitializerConfig config) {

    }

    @Override
    public Transporter buildTransporter() {
        return null;
    }

    @Override
    public void bindHandlers(List<ActorInfo> actorInfos) {

    }

    @Override
    public void close() throws IOException {

    }
}
