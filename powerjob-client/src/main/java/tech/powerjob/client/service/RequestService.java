package tech.powerjob.client.service;

import java.io.Closeable;

/**
 * 请求服务
 *
 * @author tjq
 * @since 2024/2/20
 */
public interface RequestService extends Closeable {


    String request(String path, PowerRequestBody powerRequestBody);
}
