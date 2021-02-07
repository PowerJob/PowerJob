package com.github.kfcfans.powerjob.server.transport;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.Protocol;

/**
 * Transporter
 *
 * @author tjq
 * @since 2021/2/7
 */
public interface Transporter {

    Protocol getProtocol();

    String getAddress();

    void transfer(String address, OmsSerializable object);
}
