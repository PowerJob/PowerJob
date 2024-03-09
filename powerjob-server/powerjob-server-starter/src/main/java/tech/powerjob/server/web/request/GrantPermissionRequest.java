package tech.powerjob.server.web.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 授权请求
 *
 * @author tjq
 * @since 2024/2/12
 */
@Data
public class GrantPermissionRequest implements Serializable {

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 授予的角色
     */
    private Integer role;

    /**
     * 授予的用户IDS
     */
    private List<Long> userIds;
}
