package tech.powerjob.server.auth.dp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.interceptor.DynamicPermissionPlugin;
import tech.powerjob.server.web.request.GrantPermissionRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * 授权动态权限计算
 * 授予权限要低于或等于授权人自身的权限
 *
 * @author tjq
 * @since 2024/2/12
 */
@Slf4j
public class GrantDynamicPermission implements DynamicPermissionPlugin {
    @Override
    public Permission calculate(HttpServletRequest request, Object handler) {
        try {
            //获取请求body
            byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
            String body = new String(bodyBytes, request.getCharacterEncoding());
            GrantPermissionRequest grantPermissionRequest = JsonUtils.parseObject(body, GrantPermissionRequest.class);
            Role role = Role.of(grantPermissionRequest.getRole());

            switch (role) {
                case OBSERVER: return Permission.READ;
                case QA: return Permission.OPS;
                case DEVELOPER: return Permission.WRITE;
            }

        } catch (Exception e) {
            log.error("[GrantDynamicPermission] check permission failed, please fix the bug!!!", e);
        }

        return Permission.SU;
    }
}
