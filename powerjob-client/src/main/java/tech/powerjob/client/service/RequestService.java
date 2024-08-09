package tech.powerjob.client.service;

/**
 * 请求服务
 *
 * @author tjq
 * @since 2024/2/20
 */
public interface RequestService {


    String request(String path, Object body);
}
