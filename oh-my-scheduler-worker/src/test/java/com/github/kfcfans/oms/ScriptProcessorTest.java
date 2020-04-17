package com.github.kfcfans.oms;

import com.github.kfcfans.oms.worker.core.processor.built.PythonProcessor;
import com.github.kfcfans.oms.worker.core.processor.built.ShellProcessor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试脚本处理器
 *
 * @author tjq
 * @since 2020/4/15
 */
public class ScriptProcessorTest {

    private static final long timeout = 10000;
    private static final ExecutorService pool = Executors.newFixedThreadPool(3);

    @Test
    public void testLocalShellProcessor() throws Exception {
        ShellProcessor sp = new ShellProcessor(1L, "ls -a", timeout, pool);
        sp.process(null);

        ShellProcessor sp2 = new ShellProcessor(2777L, "pwd", timeout, pool);
        sp2.process(null);
    }

    @Test
    public void testLocalPythonProcessor() throws Exception {
        PythonProcessor pp = new PythonProcessor(2L, "print 'Hello World!'", timeout, pool);
        pp.process(null);
    }

    @Test
    public void testNetShellProcessor() throws Exception {
        ShellProcessor sp = new ShellProcessor(18L, "http://localhost:8080/test/test.sh", timeout, pool);
        sp.process(null);
    }

}
