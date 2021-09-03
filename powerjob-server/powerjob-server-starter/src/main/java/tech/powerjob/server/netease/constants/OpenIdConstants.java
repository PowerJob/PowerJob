package tech.powerjob.server.netease.constants;

/**
 * @author Echo009
 * @since 2021/9/2
 *
 * https://login.netease.com/download/oidc_docs/flow/authorization_request.html
 */
public class OpenIdConstants {
    /**
     * 	必须	固定字符串：code，表示 Authorization Code Flow
     */
    public static final String RESPONSE_TYPE = "response_type";
    /**
     * 	必须	如：openid fullname，其中 openid为必须，多个 scope 值使用空格分隔。注意，如果scope不声明相应的权限，后续获取 userinfo 将无法获取对应的信息，如scope=openid fullname，userinfo 将只返回 用户姓名，而其它信息等将不会返回。详细 scope 支持列表见 “scope 信息定义” 表格。
     */
    public static final String SCOPE = "scope";
    /**
     * 必须	申请接入 OpenID Provider 时获取的 client id。
     */
    public static final String CLIENT_ID = "client_id";
    /**
     * 必须	用户登录成功后 OpenID Provider 将把用户重定向到该地址。
     */
    public static final String REDIRECT_URI = "redirect_uri";
    /**
     * 可选	当值设置为touch时，表示在手机或平板等移动设备上使用，登录页面将只显示登录表单，以提升交用户交互体验。
     */
    public static final String DISPLAY = "display";

    private OpenIdConstants(){

    }

}
