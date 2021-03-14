package tech.powerjob.worker.log.impl;

import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.log.OmsLogger;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;


/**
 * PowerJob 在线日志，直接上报到 Server，可在控制台直接查看
 *
 * @author tjq
 * @since 2020/4/21
 */
@AllArgsConstructor
public class OmsServerLogger implements OmsLogger {

    private final long instanceId;
    private final OmsLogHandler omsLogHandler;

    @Override
    public void debug(String messagePattern, Object... args) {
        process(LogLevel.DEBUG, messagePattern, args);
    }

    @Override
    public void info(String messagePattern, Object... args) {
        process(LogLevel.INFO, messagePattern, args);
    }

    @Override
    public void warn(String messagePattern, Object... args) {
        process(LogLevel.WARN, messagePattern, args);
    }

    @Override
    public void error(String messagePattern, Object... args) {
        process(LogLevel.ERROR, messagePattern, args);
    }

    /**
     * 生成日志内容
     * @param messagePattern 日志格式
     * @param arg 填充参数
     * @return 生成完毕的日志内容
     */
    private static String genLogContent(String messagePattern, Object... arg) {
        // 借用 Slf4J 直接生成日志信息
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(messagePattern, arg);
        if (formattingTuple.getThrowable() != null) {
            String stackTrace = ExceptionUtils.getStackTrace(formattingTuple.getThrowable());
            return formattingTuple.getMessage() + System.lineSeparator() + stackTrace;
        }else {
            return formattingTuple.getMessage();
        }
    }

    private void process(LogLevel level, String messagePattern, Object... args) {
        String logContent = genLogContent(messagePattern, args);
        omsLogHandler.submitLog(instanceId, level, logContent);
    }

}