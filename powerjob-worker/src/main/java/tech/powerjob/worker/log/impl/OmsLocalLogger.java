package tech.powerjob.worker.log.impl;

import tech.powerjob.worker.log.OmsLogger;
import lombok.extern.slf4j.Slf4j;

/**
 * for local test
 *
 * @author tjq
 * @since 2021/2/4
 */
@Slf4j
public class OmsLocalLogger implements OmsLogger {
    @Override
    public void debug(String messagePattern, Object... args) {
        log.debug(messagePattern, args);
    }

    @Override
    public void info(String messagePattern, Object... args) {
        log.info(messagePattern, args);
    }

    @Override
    public void warn(String messagePattern, Object... args) {
        log.warn(messagePattern, args);
    }

    @Override
    public void error(String messagePattern, Object... args) {
        log.error(messagePattern, args);
    }
}
