package com.github.kfcfans.oms.server.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统一处理 web 层异常信息
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler {

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResultDTO<Void> exceptionHandler(Exception e) {
        log.error("[ControllerException] http request failed.", e);
        return ResultDTO.failed(e.getMessage());
    }
}
