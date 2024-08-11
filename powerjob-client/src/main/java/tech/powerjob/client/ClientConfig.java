package tech.powerjob.client;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import tech.powerjob.client.common.Protocol;
import tech.powerjob.client.extension.ClientExtension;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 客户端配置
 *
 * @author 程序帕鲁
 * @since 2024/2/20
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class ClientConfig implements Serializable {

    /**
     * 执行器 AppName
     */
    private String appName;

    /**
     * 执行器密码
     */
    private String password;

    /**
     * 地址列表，支持格式：
     *  - IP:Port, eg: 192.168.1.1:7700
     *  - 域名, eg: powerjob.apple-inc.com
     */
    private List<String> addressList;

    /**
     * 客户端通讯协议
     */
    private Protocol protocol = Protocol.HTTP;

    /**
     * 连接超时时间
     */
    private Integer connectionTimeout;
    /**
     * 指定了等待服务器响应数据的最长时间。更具体地说，这是从服务器开始返回响应数据（包括HTTP头和数据）后，客户端读取数据的超时时间
     */
    private Integer readTimeout;
    /**
     * 指定了向服务器发送数据的最长时间。这是从客户端开始发送数据（如POST请求的正文）到数据完全发送出去的时间
     */
    private Integer writeTimeout;

    /**
     * 默认携带的请求头
     * 用于流量被基础设施识别
     */
    private Map<String, String> defaultHeaders;

    /**
     * 客户端行为扩展
     */
    private ClientExtension clientExtension;
}
