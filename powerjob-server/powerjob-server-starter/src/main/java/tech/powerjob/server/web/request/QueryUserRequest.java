package tech.powerjob.server.web.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询请求
 *
 * @author tjq
 * @since 2024/3/16
 */
@Data
public class QueryUserRequest implements Serializable {

    /**
     * 通过 userId 精确查询
     */
    private Long userIdEq;

    private String accountTypeEq;

    /**
     * nick 模糊查询
     */
    private String nickLike;

    /**
     * 手机号模糊查询
     */
    private String phoneLike;
}
