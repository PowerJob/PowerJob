package tech.powerjob.server.auth.jwt;

/**
 * JWT 安全性的核心
 * 对安全性有要求的接入方，可以自行重新该方法，自定义自己的安全 token 生成策略
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface SecretProvider {

    String fetchSecretKey();
}
