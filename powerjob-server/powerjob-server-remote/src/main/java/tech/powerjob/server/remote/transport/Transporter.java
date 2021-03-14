package tech.powerjob.server.remote.transport;

import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.response.AskResponse;

/**
 * Transporter
 *
 * @author tjq
 * @since 2021/2/7
 */
public interface Transporter {

    Protocol getProtocol();

    String getAddress();

    void tell(String address, PowerSerializable object);

    AskResponse ask(String address, PowerSerializable object) throws Exception;
}
