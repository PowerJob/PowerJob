package tech.powerjob.server.auth.plugin;

import tech.powerjob.server.auth.RoleScope;

/**
 * namespace 授权插件
 *
 * @author tjq
 * @since 2024/2/11
 */
public class SaveNamespaceGrantPermissionPlugin extends SaveGrantPermissionPlugin {
    @Override
    protected RoleScope fetchRuleScope() {
        return RoleScope.NAMESPACE;
    }
}
