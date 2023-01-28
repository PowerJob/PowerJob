package tech.powerjob.remote.framework.cs;

import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * client & server initializer
 *
 * @author MuBao
 * @since 2022/12/31
 */
public interface CSInitializer {

    /**
     * 类型名称，比如 akka, netty4，httpJson
     * @return 名称
     */
    String type();

    /**
     * initialize the framework
     * @param config config
     */
    void init(CSInitializerConfig config);

    /**
     * build a Transporter by based network framework
     * @return Transporter
     */
    Transporter buildTransporter();

    /**
     * bind Actor, publish handler's service
     * @param actorInfos actor infos
     */
    void bindHandlers(List<ActorInfo> actorInfos);

    void close() throws IOException;
}
