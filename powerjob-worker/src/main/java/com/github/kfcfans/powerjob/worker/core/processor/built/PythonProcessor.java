package com.github.kfcfans.powerjob.worker.core.processor.built;

/**
 * Python 处理器
 *
 * @author tjq
 * @since 2020/4/16
 */
public class PythonProcessor extends ScriptProcessor {

    public PythonProcessor(Long instanceId, String processorInfo, long timeout) throws Exception {
        super(instanceId, processorInfo, timeout);
    }

    @Override
    protected String genScriptName() {
        return String.format("python_%d.py", instanceId);
    }

    @Override
    protected String fetchRunCommand() {
        return "python";
    }
}
