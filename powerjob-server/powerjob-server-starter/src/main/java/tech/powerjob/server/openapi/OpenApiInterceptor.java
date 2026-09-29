package tech.powerjob.server.openapi;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.openapi.security.OpenApiSecurityService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Set;

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

    private static final Set<String> IGNORE_OPEN_API_PATH = Sets.newHashSet(OpenAPIConstant.ASSERT, OpenAPIConstant.AUTH_APP);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {

        if (!enableOpenApiAuth) {
            response.addHeader(OpenAPIConstant.RESPONSE_HEADER_AUTH_STATUS, Boolean.TRUE.toString());
            return true;
        }

        // 鉴权类请求跳过拦截
        String requestURI = request.getRequestURI();
        for (String endPath : IGNORE_OPEN_API_PATH) {
            if (requestURI.endsWith(endPath)) {
                return true;
            }
        }

        try {
            openApiSecurityService.authAppByToken(request);
            response.addHeader(OpenAPIConstant.RESPONSE_HEADER_AUTH_STATUS, Boolean.TRUE.toString());
        } catch (PowerJobException pje) {
            response.addHeader(OpenAPIConstant.RESPONSE_HEADER_AUTH_STATUS, Boolean.FALSE.toString());
            writeResponse(PowerResultDTO.f(pje), response);
            return false;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResponse(PowerResultDTO.f(e), response);

            log.error("[OpenApiInterceptor] unknown exception when auth app by token", e);

            return false;
        }

        return true;
    }

    @SneakyThrows
    private void writeResponse( PowerResultDTO<Object> powerResult, HttpServletResponse response) {

        // 设置响应的 Content-Type
        response.setContentType(OmsConstant.JSON_MEDIA_TYPE);

        // 将 JSON 写入响应
        PrintWriter writer = response.getWriter();
        writer.write(JsonUtils.toJSONString(powerResult));
        writer.flush();
    }

}
