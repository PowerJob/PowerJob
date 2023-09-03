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
}
