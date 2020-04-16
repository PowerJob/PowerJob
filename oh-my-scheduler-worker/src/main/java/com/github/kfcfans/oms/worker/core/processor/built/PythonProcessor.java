package com.github.kfcfans.oms.worker.core.processor.built;

/**
 * Python 处理器
 *
 * @author tjq
 * @since 2020/4/16
 */
public class PythonProcessor extends ScriptProcessor {

    public PythonProcessor(Long instanceId, String processorInfo) throws Exception {
        super(instanceId, processorInfo);
    }

    @Override
    protected String genScriptPath(Long instanceId) {
        return String.format("~/oms/script/python/%d.py", instanceId);
    }

    @Override
    protected String fetchRunCommand() {
        return "python";
    }
}
