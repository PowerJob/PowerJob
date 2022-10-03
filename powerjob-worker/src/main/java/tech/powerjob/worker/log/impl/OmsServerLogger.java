package tech.powerjob.worker.log.impl;

import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.worker.background.OmsLogHandler;


/**
 * WARN：Please do not use this logger to print large amounts of logs! <br/>
 * WARN：Please do not use this logger to print large amounts of logs! <br/>
 * WARN：Please do not use this logger to print large amounts of logs! <br/>
 *
 * @author tjq
 * @since 2022/9/16
 */
public class OmsServerLogger extends AbstractOmsLogger {

    private final long instanceId;
    private final OmsLogHandler omsLogHandler;

    public OmsServerLogger(LogConfig logConfig, long instanceId, OmsLogHandler omsLogHandler) {
        super(logConfig);
        this.instanceId = instanceId;
        this.omsLogHandler = omsLogHandler;
    }

    @Override
    public void debug0(String messagePattern, Object... args) {
        process(LogLevel.DEBUG, messagePattern, args);
    }

    @Override
    public void info0(String messagePattern, Object... args) {
        process(LogLevel.INFO, messagePattern, args);
    }

    @Override
    public void warn0(String messagePattern, Object... args) {
        process(LogLevel.WARN, messagePattern, args);
    }

    @Override
    public void error0(String messagePattern, Object... args) {
        process(LogLevel.ERROR, messagePattern, args);
    }

    private void process(LogLevel level, String messagePattern, Object... args) {
        String logContent = genLogContent(messagePattern, args);
        omsLogHandler.submitLog(instanceId, level, logContent);
    }

}