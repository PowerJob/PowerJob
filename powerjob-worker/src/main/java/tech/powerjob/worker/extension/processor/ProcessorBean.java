package tech.powerjob.worker.extension.processor;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 处理器对象
 *
 * @author Echo009
 * @since 2022/9/23
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProcessorBean {

    /**
     * 真正用来执行逻辑的处理器对象
     */
    private transient BasicProcessor processor;
    /**
     * 加载该处理器对象的 classLoader，可空，空则使用 {@link Object#getClass()#getClassLoader() 代替}
     */
    private transient ClassLoader classLoader;

    /**
     * Bean 是否稳定
     * SpringBean / 普通Java 对象，在整个 JVM 生命周期内都不会变，可声明为稳定，在上层缓存，避免每次都要重现 build processor
     * 对于动态容器，可能在部署后改变，则需要声明为不稳定
     */
    private boolean stable = true;
}
