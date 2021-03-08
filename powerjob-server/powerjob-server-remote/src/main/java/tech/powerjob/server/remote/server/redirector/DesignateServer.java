package tech.powerjob.server.remote.server.redirector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要在指定的服务器运行
 * 注意：该注解所在方法的参数必须为对象，不可以是 long 等基本类型
 *
 * @author tjq
 * @since 12/13/20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DesignateServer {

    /**
     * 转发请求需要 AppInfo 下的 currentServer 信息，因此必须要有 appId 作为入参，该字段指定了 appId 字段的参数名称，默认为 appId
     * @return appId 参数名称
     */
    String appIdParameterName() default "appId";
}
