package com.github.kfcfans.oms;

import com.github.kfcfans.oms.worker.core.executor.ShellProcessor;
import org.junit.jupiter.api.Test;

/**
 * 测试脚本处理器
 *
 * @author tjq
 * @since 2020/4/15
 */
public class ScriptProcessorTest {

    @Test
    public void testShellProcessor() throws Exception {
        ShellProcessor sp = new ShellProcessor(277L, "ls -a");
        sp.process(null);

        ShellProcessor sp2 = new ShellProcessor(277L, "pwd");
        sp2.process(null);
    }

}
