package com.github.kfcfans.powerjob.worker.core.processor.built;

import lombok.extern.slf4j.Slf4j;

/**
 * Shell 处理器
 * 由 ProcessorTracker 创建
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
public class ShellProcessor extends ScriptProcessor {

    public ShellProcessor(Long instanceId, String processorInfo, long timeout) throws Exception {
        super(instanceId, processorInfo, timeout);
    }

    @Override
    protected String genScriptName() {
        return String.format("shell_%d.sh", instanceId);
    }

    @Override
    protected String fetchRunCommand() {
        return "/bin/sh";
    }
}
