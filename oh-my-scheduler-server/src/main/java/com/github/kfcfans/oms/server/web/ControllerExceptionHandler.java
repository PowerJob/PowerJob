package com.github.kfcfans.oms.server.web;

import com.github.kfcfans.oms.common.OmsException;
import com.github.kfcfans.oms.common.response.ResultDTO;
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

        // 不是所有异常都需要打印完整堆栈，后续可以定义内部的Exception，便于判断
        if (e instanceof IllegalArgumentException || e instanceof OmsException) {
            log.warn("[ControllerException] http request failed, message is {}.", e.getMessage());
        }else {
            log.error("[ControllerException] http request failed.", e);
        }
        return ResultDTO.failed(e.getMessage());
    }
}
