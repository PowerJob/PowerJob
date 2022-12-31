package tech.powerjob.remote.framework.base;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * URL
 *
 * @author tjq
 * @since 2022/12/31
 */
@Data
@Accessors(chain = true)
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
