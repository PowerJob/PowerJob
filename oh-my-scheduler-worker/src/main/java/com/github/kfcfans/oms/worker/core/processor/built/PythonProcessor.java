package com.github.kfcfans.oms.worker.core.processor.built;

import java.util.concurrent.ExecutorService;

/**
 * Python 处理器
 *
 * @author tjq
 * @since 2020/4/16
 */
public class PythonProcessor extends ScriptProcessor {

    public PythonProcessor(Long instanceId, String processorInfo, long timeout, ExecutorService pool) throws Exception {
        super(instanceId, processorInfo, timeout, pool);
    }

    @Override
    protected String genScriptName(Long instanceId) {
        return String.format("python_%d.py", instanceId);
    }

    @Override
    protected String fetchRunCommand() {
        return "python";
    }
}
