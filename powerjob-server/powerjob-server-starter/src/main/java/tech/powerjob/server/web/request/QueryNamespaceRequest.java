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
    private String codeLike;

    /**
     * 名称模糊查询
     */
    private String nameLike;

    private String tagLike;

    /* ****************** 分页参数  ****************** */
    /**
     * 当前页码
     */
    private Integer index = 0;
    /**
     * 页大小
     */
    private Integer pageSize = 10;
}
