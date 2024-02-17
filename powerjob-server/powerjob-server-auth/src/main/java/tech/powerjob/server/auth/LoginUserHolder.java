package tech.powerjob.server.auth;

/**
 * LoginUserHolder
 *
 * @author tjq
 * @since 2023/4/16
 */
public class LoginUserHolder {

    private static final ThreadLocal<PowerJobUser> TL = new ThreadLocal<>();

    public static PowerJobUser get() {
        return TL.get();
    }

    public static void set(PowerJobUser powerJobUser) {
        TL.set(powerJobUser);
    }

    public static void clean() {
        TL.remove();
    }

    /**
     * 获取用户名
     * @return 存在则返回常规用户名，否则返回 unknown
     */
    public static String getUserName() {
        PowerJobUser powerJobUser = get();
        if (powerJobUser != null) {
            return powerJobUser.getUsername();
        }
        return "UNKNOWN";
    }

    /**
     * 获取用户ID
     * @return 存在则返回，否则返回 null
     */
    public static Long getUserId() {
        PowerJobUser powerJobUser = get();
        if (powerJobUser != null) {
            return powerJobUser.getId();
        }
        return null;
    }
}
