package com.github.kfcfans.oms.worker.core.processor.built;

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


    public ShellProcessor(Long instanceId, String processorInfo) throws Exception {
        super(instanceId, processorInfo);
    }

    @Override
    protected String genScriptPath(Long instanceId) {
        return String.format("~/oms/script/shell/%d.sh", instanceId);
    }

    @Override
    protected String fetchRunCommand() {
        return "/bin/sh";
    }
}
