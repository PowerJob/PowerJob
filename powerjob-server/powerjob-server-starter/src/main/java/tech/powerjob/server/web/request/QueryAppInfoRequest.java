package tech.powerjob.server.web.request;

import lombok.Data;

/**
 * 查询应用信息
 *
 * @author tjq
 * @since 2024/2/11
 */
@Data
public class QueryAppInfoRequest {

    /**
     * appId 精确查旋
     */
    private Long appId;
    /**
     * namespaceId
     */
    private Long namespaceId;
    /**
     * 任务名称
     */
    private String appNameLike;

    private String tagLike;

    /**
     * 查询与我相关的任务（我有直接权限的）
     */
    private Boolean showMyRelated;

    /* ****************** 分页参数  ****************** */
    /**
     * 当前页码
     */
    private Integer index;
    /**
     * 页大小
     */
    private Integer pageSize;

}
