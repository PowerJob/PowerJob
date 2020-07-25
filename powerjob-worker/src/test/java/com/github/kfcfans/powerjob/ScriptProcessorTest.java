package com.github.kfcfans.powerjob;

import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.built.PythonProcessor;
import com.github.kfcfans.powerjob.worker.core.processor.built.ShellProcessor;
import com.github.kfcfans.powerjob.worker.log.impl.OmsServerLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试脚本处理器
 *
 * @author tjq
 * @since 2020/4/15
 */
public class ScriptProcessorTest {

    private static final long timeout = 10000;

    private static final TaskContext context = new TaskContext();

    @BeforeAll
    public static void initContext() {
        context.setOmsLogger(new OmsServerLogger(1L));
    }

    @Test
    public void testLocalShellProcessor() throws Exception {
        ShellProcessor sp = new ShellProcessor(1L, "ls -a", timeout);
        System.out.println(sp.process(context));

        ShellProcessor sp2 = new ShellProcessor(2777L, "pwd", timeout);
        System.out.println(sp2.process(context));
    }

    @Test
    public void testLocalPythonProcessor() throws Exception {
        PythonProcessor pp = new PythonProcessor(2L, "print 'Hello World!'", timeout);
        System.out.println(pp.process(context));
    }

    @Test
    public void testNetShellProcessor() throws Exception {
        ShellProcessor sp = new ShellProcessor(18L, "http://localhost:8080/test/test.sh", timeout);
        System.out.println(sp.process(context));
    }

    @Test
    public void testFailedScript() throws Exception {
        ShellProcessor sp3 = new ShellProcessor(ThreadLocalRandom.current().nextLong(), "mvn tjq", timeout);
        System.out.println(sp3.process(context));
    }
}
