package tech.powerjob.remote.framework.base;

import java.io.Serializable;

/**
 * URL
 *
 * @author tjq
 * @since 2022/12/31
 */
public class URL implements Serializable {
    /**
     * remote address
     */
    private Address address;

    /**
     * location
     */
    private HandlerLocation location;
}
