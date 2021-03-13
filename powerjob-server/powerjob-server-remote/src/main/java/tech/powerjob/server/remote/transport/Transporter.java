package tech.powerjob.server.remote.transport;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.Protocol;
import com.github.kfcfans.powerjob.common.response.AskResponse;

/**
 * Transporter
 *
 * @author tjq
 * @since 2021/2/7
 */
public interface Transporter {

    Protocol getProtocol();

    String getAddress();

    void tell(String address, OmsSerializable object);

    AskResponse ask(String address, OmsSerializable object) throws Exception;
}
