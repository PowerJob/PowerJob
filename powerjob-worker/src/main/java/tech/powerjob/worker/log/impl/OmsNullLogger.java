package tech.powerjob.worker.log.impl;

import tech.powerjob.worker.log.OmsLogger;

/**
 * DO NOTHING
 *
 * @author tjq
 * @since 2022/10/3
 */
public class OmsNullLogger implements OmsLogger {

    @Override
    public void debug(String messagePattern, Object... args) {
    }

    @Override
    public void info(String messagePattern, Object... args) {
    }

    @Override
    public void warn(String messagePattern, Object... args) {
    }

    @Override
    public void error(String messagePattern, Object... args) {
    }
}
