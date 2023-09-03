package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户基础信息
 *
 * @author tjq
 * @since 2023/9/3
 */
@Getter
@Setter
@NoArgsConstructor
public class UserBaseVO {
    private Long id;
    private String username;
    private String nick;
}
