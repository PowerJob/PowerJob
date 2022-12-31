package tech.powerjob.remote.framework.cs;

import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.io.Closeable;
import java.util.List;

/**
 * client & server initializer
 *
 * @author tjq
 * @since 2022/12/31
 */
public interface CSInitializer extends Closeable {

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
     * @param handlerInfos handler infos
     */
    void bindHandlers(List<HandlerInfo> handlerInfos);
}
