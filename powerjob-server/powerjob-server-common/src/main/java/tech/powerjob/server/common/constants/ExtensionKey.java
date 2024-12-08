package tech.powerjob.server.common.constants;

/**
 * 扩展 key
 *
 * @author tjq
 * @since 2024/12/8
 */
public interface ExtensionKey {

    interface App {
        String allowedBecomeAdminByPassword = "allowedBecomeAdminByPassword";
    }

    interface PwjbUser {
        String allowedChangePwd = "allowedChangePwd";
    }
}
