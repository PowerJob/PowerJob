package tech.powerjob.worker.log.impl;

import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.worker.log.OmsLogger;

/**
 * AbstractOmsLogger
 *
 * @author tjq
 * @since 2022/9/16
 */
public abstract class AbstractOmsLogger implements OmsLogger {

    private final LogConfig logConfig;

    public AbstractOmsLogger(LogConfig logConfig) {
        this.logConfig = logConfig;

        // 兼容空数据场景，添加默认值，尽量与原有逻辑保持兼容
        if (logConfig.getLevel() == null) {
            logConfig.setLevel(LogLevel.INFO.getV());
        }
        if (logConfig.getType() == null) {
            logConfig.setType(LogConfig.LogType.ONLINE.getV());
        }
    }

    abstract void debug0(String messagePattern, Object... args);

    abstract void info0(String messagePattern, Object... args);

    abstract void warn0(String messagePattern, Object... args);

    abstract void error0(String messagePattern, Object... args);

    @Override
    public void debug(String messagePattern, Object... args) {
        if (LogLevel.DEBUG.getV() < logConfig.getLevel()) {
            return;
        }
        debug0(messagePattern, args);
    }

    @Override
    public void info(String messagePattern, Object... args) {
        if (LogLevel.INFO.getV() < logConfig.getLevel()) {
            return;
        }
        info0(messagePattern, args);
    }

    @Override
    public void warn(String messagePattern, Object... args) {
        if (LogLevel.WARN.getV() < logConfig.getLevel()) {
            return;
        }
        warn0(messagePattern, args);
    }

    @Override
    public void error(String messagePattern, Object... args) {
        if (LogLevel.ERROR.getV() < logConfig.getLevel()) {
            return;
        }
        error0(messagePattern, args);
    }
}
