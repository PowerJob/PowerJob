package tech.powerjob.worker.log.impl;

import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.model.LogConfig;

/**
 * use java.lang.System#out or java.lang.System#err to print log info
 *
 * @author tjq
 * @since 2022/10/3
 */
public class OmsStdOutLogger extends AbstractOmsLogger {

    private static final String PREFIX = "[PowerJob] [%s] ";

    public OmsStdOutLogger(LogConfig logConfig) {
        super(logConfig);
    }

    @Override
    void debug0(String messagePattern, Object... args) {
        System.out.println(buildStdOut(LogLevel.DEBUG, messagePattern, args));
    }

    @Override
    void info0(String messagePattern, Object... args) {
        System.out.println(buildStdOut(LogLevel.INFO, messagePattern, args));
    }

    @Override
    void warn0(String messagePattern, Object... args) {
        System.out.println(buildStdOut(LogLevel.WARN, messagePattern, args));
    }

    @Override
    void error0(String messagePattern, Object... args) {
        System.err.println(buildStdOut(LogLevel.ERROR, messagePattern, args));
    }

    private static String buildStdOut(LogLevel logLevel, String messagePattern, Object... args) {
        return String.format(PREFIX, logLevel.name()).concat(genLogContent(messagePattern, args));
    }
}
