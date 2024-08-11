package tech.powerjob.server.auth.plugin;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.GrantPermissionPlugin;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.common.utils.SpringUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * WEB 类保存&修改一体型请求-授权插件
 *
 * @author tjq
 * @since 2024/2/11
 */
@Slf4j
public abstract class SaveGrantPermissionPlugin implements GrantPermissionPlugin {

    private static final String KEY_ID = "id";

    @Override
    public void grant(Object[] args, Object result, Method method, Object originBean) {

        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("[GrantPermission] args not match, maybe there has some bug");
        }

        // 理论上不可能，前置已完成判断
        PowerJobUser powerJobUser = LoginUserHolder.get();
        if (powerJobUser == null) {
            throw new IllegalArgumentException("[GrantPermission] user not login, can't grant permission");
        }

        // 解析ID，非空代表更新，不授权
        Map<String, Object> saveRequest = JsonUtils.parseMap(JsonUtils.toJSONString(args[0]));
        Long id = MapUtils.getLong(saveRequest, KEY_ID);
        if (id != null) {
            return;
        }

        if (!(result instanceof ResultDTO)) {
            throw new IllegalArgumentException("[GrantPermission] result not instanceof ResultDTO, maybe there has some bug");
        }

        ResultDTO<?> resultDTO = (ResultDTO<?>) result;

        if (!resultDTO.isSuccess()) {
            log.warn("[GrantPermission] result not success, skip grant permission!");
            return;
        }

        Map<String, Object> saveResult = JsonUtils.parseMap(JsonUtils.toJSONString(resultDTO.getData()));
        Long savedId = MapUtils.getLong(saveResult, KEY_ID);
        if (savedId == null) {
            throw new IllegalArgumentException("[GrantPermission] result success but id not exits, maybe there has some bug, please fix it!!!");
        }

        PowerJobPermissionService powerJobPermissionService = SpringUtils.getBean(PowerJobPermissionService.class);

        Map<String, Object> extra = Maps.newHashMap();
        extra.put("source", "SaveGrantPermissionPlugin");

        powerJobPermissionService.grantRole(fetchRuleScope(), savedId, powerJobUser.getId(), Role.ADMIN, JsonUtils.toJSONString(extra));
    }

    protected abstract RoleScope fetchRuleScope();
}
