package tech.powerjob.worker.log.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.powerjob.common.model.LogConfig;

/**
 * Many user feedback when the task volume server timeout serious. After pressure testing, we found that there is no bottleneck in the server processing scheduling tasks, and it is assumed that the large amount of logs is causing a serious bottleneck. Therefore, we need to provide local logging API for large MR tasks.
 *
 * @author tjq
 * @since 2021/2/4
 */
public class OmsLocalLogger extends AbstractOmsLogger {

    private final Logger LOGGER;

    private static final String DEFAULT_LOGGER_NAME = OmsLocalLogger.class.getName();

    public OmsLocalLogger(LogConfig logConfig) {
        super(logConfig);

        String loggerName = StringUtils.isEmpty(logConfig.getLoggerName()) ? DEFAULT_LOGGER_NAME : logConfig.getLoggerName();
        LOGGER = LoggerFactory.getLogger(loggerName);
    }

    @Override
    public void debug0(String messagePattern, Object... args) {
        LOGGER.debug(messagePattern, args);
    }

    @Override
    public void info0(String messagePattern, Object... args) {
        LOGGER.info(messagePattern, args);
    }

    @Override
    public void warn0(String messagePattern, Object... args) {
        LOGGER.warn(messagePattern, args);
    }

    @Override
    public void error0(String messagePattern, Object... args) {
        LOGGER.error(messagePattern, args);
    }
}
