package tech.powerjob.remote.benchmark;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.remote.framework.BenchmarkActor;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.remote.framework.transporter.Transporter;

import javax.annotation.PostConstruct;

/**
 * EngineService
 *
 * @author tjq
 * @since 2023/1/7
 */
@Service
public class EngineService {

    public static final String HOST = "127.0.0.1";

    public static final int SERVER_AKKA_PORT = 10001;
    public static final int SERVER_HTTP_PORT = 10002;

    public static final int CLIENT_AKKA_PORT = 20001;
    public static final int CLIENT_HTTP_PORT = 20002;

    @Getter
    private Transporter akkaTransporter;
    @Getter
    private Transporter httpTransporter;

    @PostConstruct
    public void init() {
        // http server
        new PowerJobRemoteEngine().start(new EngineConfig()
                .setServerType(ServerType.SERVER)
                .setActorList(Lists.newArrayList(new BenchmarkActor()))
                .setType(Protocol.HTTP.name())
                .setBindAddress(new Address().setHost(HOST).setPort(SERVER_HTTP_PORT)));

        // akka server
        new PowerJobRemoteEngine().start(new EngineConfig()
                .setServerType(ServerType.SERVER)
                .setActorList(Lists.newArrayList(new BenchmarkActor()))
                .setType(Protocol.AKKA.name())
                .setBindAddress(new Address().setHost(HOST).setPort(SERVER_AKKA_PORT)));

        // http client
        httpTransporter = new PowerJobRemoteEngine().start(new EngineConfig()
                .setServerType(ServerType.WORKER)
                .setActorList(Lists.newArrayList(new BenchmarkActor()))
                .setType(Protocol.HTTP.name())
                .setBindAddress(new Address().setHost(HOST).setPort(CLIENT_HTTP_PORT))).getTransporter();

        // akka client
        akkaTransporter = new PowerJobRemoteEngine().start(new EngineConfig()
                .setServerType(ServerType.WORKER)
                .setActorList(Lists.newArrayList(new BenchmarkActor()))
                .setType(Protocol.AKKA.name())
                .setBindAddress(new Address().setHost(HOST).setPort(CLIENT_AKKA_PORT))).getTransporter();
    }
}
