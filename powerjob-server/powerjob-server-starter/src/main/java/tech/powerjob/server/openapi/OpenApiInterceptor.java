package tech.powerjob.server.openapi;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.openapi.security.OpenApiSecurityService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * OpenAPI 拦截器
 *
 * @author 程序帕鲁
 * @since 2024/2/19
 */
@Slf4j
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    @Resource
    private OpenApiSecurityService openApiSecurityService;

    /**
     * 4.x 及前序版本的 OpenAPI 均为携带 auth 的必要参数，直接开启鉴权功能会导致之前的服务全部报错
     * 因此提供功能开关给到使用者，若无安全影响，可展示关闭鉴权功能，等 client 升级完毕后再打开鉴权
     */
    @Value("${oms.auth.openapi.enable:false}")
    private boolean enableOpenApiAuth;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        if (!enableOpenApiAuth) {
            return true;
        }

        try {
            openApiSecurityService.authAppByToken(request);
        } catch (PowerJobException pje) {
            PowerResultDTO<Object> ret = PowerResultDTO.f(pje);
            writeResponse(JsonUtils.toJSONString(ret), response);
            return false;
        } catch (Exception e) {
            PowerResultDTO<Object> ret = PowerResultDTO.f(e);
            writeResponse(JsonUtils.toJSONString(ret), response);
            return false;
        }

        return true;
    }

    @SneakyThrows
    private void writeResponse(String content, HttpServletResponse response) {
        // 设置响应状态码，通常是 400, 401, 403 等错误码
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // 设置响应的 Content-Type
        response.setContentType("application/json;charset=UTF-8");

        // 将 JSON 写入响应
        PrintWriter writer = response.getWriter();
        writer.write(content);
        writer.flush();
    }

}
