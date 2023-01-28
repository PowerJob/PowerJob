package tech.powerjob.remote.framework.actor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import tech.powerjob.remote.framework.base.HandlerLocation;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * HandlerInfo
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class HandlerInfo {

    private HandlerLocation location;
    /**
     * handler 对应的方法
     */
    private Method method;

    /**
     * Handler 注解携带的信息
     */
    private Handler anno;
}
