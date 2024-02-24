package tech.powerjob.server.web.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询任务详情的请求
 *
 * @author tjq
 * @since 2024/2/25
 */
@Data
public class QueryInstanceDetailRequest implements Serializable {

    private Long appId;

    private Long instanceId;

    /**
     * 自定义查询条件（SQL）
     */
    private String customQuery;
}
