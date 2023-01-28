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

}
