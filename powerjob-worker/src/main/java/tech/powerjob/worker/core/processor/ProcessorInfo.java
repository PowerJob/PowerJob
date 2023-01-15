package tech.powerjob.worker.core.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * @author Echo009
 * @since 2022/9/23
 */
@RequiredArgsConstructor
@Getter
public class ProcessorInfo {

    private final BasicProcessor basicProcessor;

    private final ClassLoader classLoader;

    public static ProcessorInfo of(BasicProcessor basicProcessor, ClassLoader classLoader) {
        return new ProcessorInfo(basicProcessor, classLoader);
    }
}
