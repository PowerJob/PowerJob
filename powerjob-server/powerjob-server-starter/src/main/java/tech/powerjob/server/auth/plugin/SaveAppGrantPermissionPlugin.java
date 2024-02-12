package tech.powerjob.server.auth.plugin;

import tech.powerjob.server.auth.RoleScope;

/**
 * desc
 *
 * @author tjq
 * @since 2024/2/11
 */
public class SaveAppGrantPermissionPlugin extends SaveGrantPermissionPlugin {
    @Override
    protected RoleScope fetchRuleScope() {
        return RoleScope.APP;
    }
}
