package tech.powerjob.remote.http;

import tech.powerjob.remote.framework.transporter.Protocol;

/**
 * HttpProtocol
 *
 * @author tjq
 * @since 2022/12/31
 */
public class HttpProtocol implements Protocol {

    @Override
    public String name() {
        return tech.powerjob.common.enums.Protocol.HTTP.name();
    }
}
