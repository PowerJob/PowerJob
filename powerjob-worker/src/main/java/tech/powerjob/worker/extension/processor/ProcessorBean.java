package tech.powerjob.worker.extension.processor;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 处理器对象，理论上只需要返回 BasicProcessor，但为了扩展性还是选择封装为对象
 *
 * @author tjq
 * @since 2023/1/17
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProcessorBean {

    /**
     * 真正用来执行逻辑的处理器对象
     */
    private transient BasicProcessor processor;
}
