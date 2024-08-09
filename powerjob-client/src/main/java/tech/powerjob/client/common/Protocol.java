package tech.powerjob.client.common;

import lombok.Getter;

/**
 * Protocol
 *
 * @author tjq
 * @since 2024/2/20
 */
@Getter
public enum Protocol {

    HTTP("http"),

    HTTPS("https");

    private final String protocol;

    Protocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return protocol;
    }
}
