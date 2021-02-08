package com.github.kfcfans.powerjob.common;

import java.io.Serializable;

/**
 * PowerJob serializable interface.
 *
 * @author tjq
 * @since 2020/4/16
 */
public interface OmsSerializable extends Serializable {

    /**
     * request path for http or other protocol, like 'stopInstance'
     * @return null for non-http request object or no-null path for http request needed object
     */
    default String path() {
        return null;
    }
}
