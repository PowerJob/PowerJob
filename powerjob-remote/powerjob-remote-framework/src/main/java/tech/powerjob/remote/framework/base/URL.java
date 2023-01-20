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
     * 调用的集群类型（用于兼容 AKKA 等除了IP还需要指定 system 访问的情况）
     */
    private ServerType serverType;

    /**
     * remote address
     */
    private Address address;

    /**
     * location
     */
    private HandlerLocation location;
}
