package tech.powerjob.client.extension;

import java.util.List;

/**
 * 扩展服务
 *
 * @author tjq
 * @since 2024/8/11
 */
public interface ClientExtension {

    /**
     * 动态提供地址，适用于 server 部署在动态集群上的场景
     * @param context 上下文
     * @return 地址，格式要求同 ClientConfig#addressList
     */
    List<String> addressProvider(ExtensionContext context);
}
