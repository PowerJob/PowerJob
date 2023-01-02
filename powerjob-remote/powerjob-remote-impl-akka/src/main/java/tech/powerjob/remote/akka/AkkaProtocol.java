package tech.powerjob.remote.akka;

import tech.powerjob.remote.framework.transporter.Protocol;

/**
 * AkkaProtocol
 *
 * @author tjq
 * @since 2022/12/31
 */
public class AkkaProtocol implements Protocol {
    @Override
    public String name() {
        return tech.powerjob.common.enums.Protocol.AKKA.name();
    }
}
