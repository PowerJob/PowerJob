package tech.powerjob.samples.tester;

import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 测试直接使用 BeanName 获取处理器
 * 控制台可填写 powerJobTestBeanNameProcessor 作为处理器信息
 *
 * @author tjq
 * @since 2023/3/5
 */
@Component(value = "powerJobTestBeanNameProcessor")
public class TestFindByBeanNameProcessor implements BasicProcessor {
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("======== IN =======");
        return new ProcessResult(true, "Welcome to use PowerJob~");
    }
}
