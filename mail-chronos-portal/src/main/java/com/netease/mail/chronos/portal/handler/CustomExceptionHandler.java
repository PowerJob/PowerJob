package com.netease.mail.chronos.portal.handler;

import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.response.BaseResponse;
import com.netease.mail.chronos.base.utils.ExceptionUtil;
import com.netease.mail.quark.commons.serialization.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@SuppressWarnings("rawtypes")
@ControllerAdvice(annotations = {RestController.class})
@Slf4j
public class CustomExceptionHandler {


    @ExceptionHandler(value = Exception.class)
    @SuppressWarnings("squid:S112")
    @ResponseBody
    public BaseResponse defaultErrorHandler(HttpServletRequest request, Exception e) throws Exception {
        // If the exception is annotated with @ResponseStatus rethrow it and let
        if (AnnotationUtils.findAnnotation
                (e.getClass(), ResponseStatus.class) != null) {
            throw e;
        }
        String method = request.getMethod();
        String url = request.getRequestURI();
        Map<String, String[]> params = request.getParameterMap();
        // transform
        BaseResponse rtn = new BaseResponse().setSuccess(false);
        if (e instanceof BaseException) {
            BaseException exception = (BaseException) e;
            rtn.setCode(exception.getCode())
                    .setDesc(exception.getMessage());
            log.warn("[opt:handleException, url:{}, method:{}, params:{}]", url, method, JacksonUtils.toString(params), e);
        } else {
            rtn.setCode(BaseStatusEnum.UNKNOWN.getCode())
                    .setDesc(BaseStatusEnum.UNKNOWN.getDesc());
            log.error("[opt:handleException, url:{}, method:{}, params:{}]", url, method, JacksonUtils.toString(params), e);
        }
        // fill error info
        rtn.setError(ExceptionUtil.getExceptionDesc(e));
        return rtn;
    }


}
