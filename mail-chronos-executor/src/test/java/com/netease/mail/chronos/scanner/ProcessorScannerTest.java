package com.netease.mail.chronos.scanner;

import com.netease.mail.chronos.executor.support.processor.RtTaskInstanceTablePartitionProcessor;
import org.junit.Assert;
import org.junit.Test;
import tech.powerjob.worker.netease.scanner.ProcessorScanner;

import java.util.Set;

/**
 * @author Echo009
 * @since 2022/1/25
 */
public class ProcessorScannerTest {

    private ProcessorScanner pc = new ProcessorScanner();

    @Test
    public void test() {
        Set<String> scan = pc.scan("com.netease.mail.*.executor");
        Assert.assertTrue(scan.contains(RtTaskInstanceTablePartitionProcessor.class.getCanonicalName()));
    }

}
