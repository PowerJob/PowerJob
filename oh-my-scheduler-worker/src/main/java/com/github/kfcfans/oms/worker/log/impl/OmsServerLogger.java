package com.github.kfcfans.oms.worker.log.impl;

import com.github.kfcfans.oms.worker.background.OmsLogHandler;
import com.github.kfcfans.oms.worker.log.OmsLogger;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;


/**
 * OhMyScheduler 在线日志，直接上报到 Server，可在控制台直接查看
 *
 * @author tjq
 * @since 2020/4/21
 */
@AllArgsConstructor
public class OmsServerLogger implements OmsLogger {

    private final long instanceId;

    // Level|业务方自身的日志
    private static final String LOG_PREFIX = "{} ";

    @Override
    public void debug(String messagePattern, Object... args) {
        process("DEBUG", messagePattern, args);
    }

    @Override
    public void info(String messagePattern, Object... args) {
        process("INFO", messagePattern, args);
    }

    @Override
    public void warn(String messagePattern, Object... args) {
        process("WARN", messagePattern, args);
    }

    @Override
    public void error(String messagePattern, Object... args) {
        process("ERROR", messagePattern, args);
    }

    /**
     * 生成日志内容
     * @param level 级别，DEBUG/INFO/WARN/ERROR
     * @param messagePattern 日志格式
     * @param arg 填充参数
     * @return 生成完毕的日志内容
     */
    private static String genLog(String level, String messagePattern, Object... arg) {

        String pattern = LOG_PREFIX + messagePattern;
        Object[] newArgs = new Object[arg.length + 2];
        newArgs[0] = level;
        System.arraycopy(arg, 0, newArgs, 1, arg.length);

        // 借用 Slf4J 直接生成日志信息
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(pattern, newArgs);
        if (formattingTuple.getThrowable() != null) {
            String stackTrace = ExceptionUtils.getStackTrace(formattingTuple.getThrowable());
            return formattingTuple.getMessage() + System.lineSeparator() + stackTrace;
        }else {
            return formattingTuple.getMessage();
        }
    }

    private void process(String level, String messagePattern, Object... args) {
        String logContent = genLog(level, messagePattern, args);
        OmsLogHandler.INSTANCE.submitLog(instanceId, logContent);
    }

}