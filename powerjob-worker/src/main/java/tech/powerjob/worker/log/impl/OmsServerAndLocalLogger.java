package tech.powerjob.worker.log.impl;

import tech.powerjob.common.model.LogConfig;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.log.OmsLogger;

/**
 * OmsServerLogger + OmsLocalLogger
 *
 * @author tjq
 * @since 2023/2/8
 */
public class OmsServerAndLocalLogger extends AbstractOmsLogger {

    private final OmsLogger omsLocalLogger;
    private final OmsLogger omsServerLogger;

    public OmsServerAndLocalLogger(LogConfig logConfig, long instanceId, OmsLogHandler omsLogHandler) {
        super(logConfig);
        this.omsLocalLogger = new OmsLocalLogger(logConfig);
        this.omsServerLogger = new OmsServerLogger(logConfig, instanceId, omsLogHandler);
    }

    @Override
    void debug0(String messagePattern, Object... args) {
        omsLocalLogger.debug(messagePattern, args);
        omsServerLogger.debug(messagePattern, args);
    }

    @Override
    void info0(String messagePattern, Object... args) {
        omsLocalLogger.info(messagePattern, args);
        omsServerLogger.info(messagePattern, args);
    }

    @Override
    void warn0(String messagePattern, Object... args) {
        omsLocalLogger.warn(messagePattern, args);
        omsServerLogger.warn(messagePattern, args);
    }

    @Override
    void error0(String messagePattern, Object... args) {
        omsLocalLogger.error(messagePattern, args);
        omsServerLogger.error(messagePattern, args);
    }
}
