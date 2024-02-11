package tech.powerjob.server.web.request;

import lombok.Data;

/**
 * 查询 namespace 请求
 *
 * @author tjq
 * @since 2024/2/11
 */
@Data
public class QueryNamespaceRequest {

    /**
     * code 模糊查询
     */
    private String code;

    /**
     * 名称模糊查询
     */
    private String name;
}
