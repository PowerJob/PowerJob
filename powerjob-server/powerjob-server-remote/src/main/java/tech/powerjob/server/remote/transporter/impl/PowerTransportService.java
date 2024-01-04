package tech.powerjob.server.remote.transporter.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.base.Address;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.engine.EngineConfig;
import tech.powerjob.remote.framework.engine.EngineOutput;
import tech.powerjob.remote.framework.engine.RemoteEngine;
import tech.powerjob.remote.framework.engine.impl.PowerJobRemoteEngine;
import tech.powerjob.server.remote.transporter.ProtocolInfo;
import tech.powerjob.server.remote.transporter.TransportService;

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
public class PowerTransportService implements TransportService, InitializingBean, DisposableBean, ApplicationContextAware {

    /**
     * server 需要激活的通讯协议，建议激活全部支持的协议
     */
    @Value("${oms.transporter.active.protocols}")
    private String activeProtocols;

    /**
     * 主要通讯协议，用于 server 与 server 之间的通讯，用户必须保证该协议可用（端口开放）！
     */
    @Value("${oms.transporter.main.protocol}")
    private String mainProtocol;

    private static final String PROTOCOL_PORT_CONFIG = "oms.%s.port";

    private final Environment environment;

    private ProtocolInfo defaultProtocol;
    private final Map<String, ProtocolInfo> protocolName2Info = Maps.newHashMap();

    private final List<RemoteEngine> engines = Lists.newArrayList();

    private ApplicationContext applicationContext;

    public PowerTransportService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public ProtocolInfo defaultProtocol() {
        return defaultProtocol;
    }

    @Override
    public Map<String, ProtocolInfo> allProtocols() {
        return protocolName2Info;
    }

    private ProtocolInfo fetchProtocolInfo(String protocol) {
        // 兼容老版 worker 未上报 protocol 的情况
        protocol = compatibleProtocol(protocol);
        final ProtocolInfo protocolInfo = protocolName2Info.get(protocol);
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

        // 从构造器注入改为从 applicationContext 获取来避免循环依赖
        final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Actor.class);
        log.info("[PowerTransportService] find Actor num={},names={}", beansWithAnnotation.size(), beansWithAnnotation.keySet());

        Address address = new Address()
                .setHost(NetUtils.getLocalHost())
                .setPort(port);
        EngineConfig engineConfig = new EngineConfig()
                .setServerType(ServerType.SERVER)
                .setType(protocol.toUpperCase())
                .setBindAddress(address)
                .setActorList(Lists.newArrayList(beansWithAnnotation.values()));
        log.info("[PowerTransportService] start to initialize RemoteEngine[type={},address={}]", protocol, address);
        RemoteEngine re = new PowerJobRemoteEngine();
        final EngineOutput engineOutput = re.start(engineConfig);
        log.info("[PowerTransportService] start RemoteEngine[type={},address={}] successfully", protocol, address);

        this.engines.add(re);
        this.protocolName2Info.put(protocol, new ProtocolInfo(protocol, address.getHost(), address.getPort(), engineOutput.getTransporter()));
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
        log.info("[PowerTransportService] ALL_PROTOCOLS: {}", protocolName2Info);
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


        this.defaultProtocol = this.protocolName2Info.get(mainProtocol);
        log.info("[PowerTransportService] chose [{}] as the default protocol, make sure this protocol can work!", mainProtocol);

        if (this.defaultProtocol == null) {
            throw new IllegalArgumentException("can't find default protocol, please check your config!");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() throws Exception {
        engines.forEach(e -> {
            try {
                e.close();
            } catch (Exception ignore) {
            }
        });
    }
}
