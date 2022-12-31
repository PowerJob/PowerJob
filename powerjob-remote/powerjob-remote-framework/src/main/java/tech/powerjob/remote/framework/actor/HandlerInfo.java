package tech.powerjob.remote.framework.actor;

import tech.powerjob.remote.framework.base.HandlerLocation;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * HandlerInfo
 *
 * @author tjq
 * @since 2022/12/31
 */
public class HandlerInfo implements Serializable {

    private HandlerLocation location;
    /**
     * handler 对应的方法
     */
    private Method method;
    /**
     * actor 对象
     */
    private Object actor;
}
