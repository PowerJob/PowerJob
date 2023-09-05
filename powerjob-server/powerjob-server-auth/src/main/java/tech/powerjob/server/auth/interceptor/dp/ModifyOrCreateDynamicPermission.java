package tech.powerjob.server.auth.interceptor.dp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.Permission;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 针对 namespace 和 app 两大鉴权纬度，创建不需要任何权限，但任何修改操作都需要 WRITE 权限
 * 创建不需要权限，修改需要校验权限
 *
 * @author tjq
 * @since 2023/9/3
 */
@Slf4j
public class ModifyOrCreateDynamicPermission implements DynamicPermission {
    @Override
    public Permission calculate(HttpServletRequest request, Object handler) {

        try {
            //获取请求body
            byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
            String body = new String(bodyBytes, request.getCharacterEncoding());

            Map<String, Object> inputParams = JsonUtils.parseMap(body);

            Object id = inputParams.get("id");

            // 创建，不需要权限
            if (id == null) {
                return Permission.NONE;
            }

            return Permission.WRITE;
        } catch (Exception e) {
            log.error("[ModifyOrCreateDynamicPermission] check permission failed, please fix the bug!!!");
        }

        // 异常情况先放行，不影响功能使用，后续修复 BUG
        return Permission.NONE;
    }
}
