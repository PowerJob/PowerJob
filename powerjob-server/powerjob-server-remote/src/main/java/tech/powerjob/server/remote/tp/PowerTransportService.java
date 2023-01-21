package tech.powerjob.server.remote.tp;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.server.remote.actoes.ServerActor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * server 数据传输服务
 *
 * @author tjq
 * @since 2023/1/21
 */
@Slf4j
@Service
public class PowerTransportService implements TransportService, InitializingBean {

    @Value("${oms.transporter.active.protocols}")
    private String activeProtocols;
    private static final String PROTOCOL_PORT_CONFIG = "oms.%s.port";

    private final Environment environment;
    private final List<ServerActor> serverActors;

    private ProtocolInfo defaultProtocol;
    private final Map<String, ProtocolInfo> protocol2Transporter = Maps.newHashMap();

    public PowerTransportService(List<ServerActor> serverActors, Environment environment) {
        this.serverActors = serverActors;
        this.environment = environment;
    }

    @Override
    public ProtocolInfo defaultProtocol() {
        return defaultProtocol;
    }

    private ProtocolInfo fetchProtocolInfo(String protocol) {
        // 兼容老版 worker 未上报 protocol 的情况
        protocol = compatibleProtocol(protocol);
        final ProtocolInfo protocolInfo = protocol2Transporter.get(protocol);
        if (protocolInfo == null) {
            throw new IllegalArgumentException("can't find Transporter by protocol :" + protocol);
        }
        return protocolInfo;
    }

    @Override
    public void tell(String protocol, URL url, PowerSerializable request) {
        fetchProtocolInfo(protocol).getTransporter().tell(url, request);
    }

    @Override
    public <T> CompletionStage<T> ask(String protocol, URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
        return fetchProtocolInfo(protocol).getTransporter().ask(url, request, clz);
    }

    private void initRemoteFrameWork(String protocol, int port) {
        Address address = new Address()
                .setHost(NetUtils.getLocalHost())
                .setPort(port);
        EngineConfig engineConfig = new EngineConfig()
                .setServerType(ServerType.SERVER)
                .setType(protocol.toUpperCase())
                .setBindAddress(address)
                .setActorList(Lists.newArrayList(serverActors));
        log.info("[PowerTransportService] start to initialize RemoteEngine[type={},address={}]", protocol, address);
        RemoteEngine re = new PowerJobRemoteEngine();
        final EngineOutput engineOutput = re.start(engineConfig);
        log.info("[PowerTransportService] start RemoteEngine[type={},address={}] successfully", protocol, address);

        this.protocol2Transporter.put(protocol, new ProtocolInfo(protocol, address.toFullAddress(), engineOutput.getTransporter()));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        log.info("[PowerTransportService] start to initialize whole PowerTransportService!");
        log.info("[PowerTransportService] activeProtocols: {}", activeProtocols);

        if (StringUtils.isEmpty(activeProtocols)) {
            throw new IllegalArgumentException("activeProtocols can't be empty!");
        }

        for (String protocol : activeProtocols.split(OmsConstant.COMMA)) {
            try {
                final int port = parseProtocolPort(protocol);
                initRemoteFrameWork(protocol, port);
            } catch (Throwable t) {
                log.error("[PowerTransportService] initialize protocol[{}] failed. If you don't need to use this protocol, you can turn it off by 'oms.transporter.active.protocols'", protocol);
                ExceptionUtils.rethrow(t);
            }
        }

        choseDefault();

        log.info("[PowerTransportService] initialize successfully!");
        log.info("[PowerTransportService] ALL_PROTOCOLS: {}", protocol2Transporter);
    }

    /**
     * 获取协议端口，考虑兼容性 & 用户仔细扩展的场景，选择动态从 env 获取 port
     * @return port
     */
    private int parseProtocolPort(String protocol) {
        final String key1 = String.format(PROTOCOL_PORT_CONFIG, protocol.toLowerCase());
        final String key2 = String.format(PROTOCOL_PORT_CONFIG, protocol.toUpperCase());
        String portStr = environment.getProperty(key1);
        if (StringUtils.isEmpty(portStr)) {
            portStr = environment.getProperty(key2);
        }
        log.info("[PowerTransportService] fetch port for protocol[{}], key={}, value={}", protocol, key1, portStr);

        if (StringUtils.isEmpty(portStr)) {
            throw new IllegalArgumentException(String.format("can't find protocol config by key: %s, please check your spring config!", key1));
        }

        return Integer.parseInt(portStr);
    }

    private String compatibleProtocol(String p) {
        if (p == null) {
            return Protocol.AKKA.name();
        }
        return p;
    }

    /**
     * HTTP 优先，否则默认取第一个协议
     */
    private void choseDefault() {
        ProtocolInfo httpP = protocol2Transporter.get(Protocol.HTTP.name());
        if (httpP != null) {
            log.info("[PowerTransportService] exist HTTP protocol, chose this as the default protocol!");
            this.defaultProtocol = httpP;
            return;
        }

        String firstProtocol = activeProtocols.split(OmsConstant.COMMA)[0];
        this.defaultProtocol = this.protocol2Transporter.get(firstProtocol);
        log.info("[PowerTransportService] chose [{}] as the default protocol!", firstProtocol);

        if (this.defaultProtocol == null) {
            throw new IllegalArgumentException("can't find default protocol, please check your config!");
        }
    }
}
