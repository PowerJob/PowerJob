package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
    protected Long id;
    protected String username;
    protected String nick;

    /**
     * 前端展示名称，更容易辨认
     */
    protected String showName;

    public void genShowName() {

        if (StringUtils.isEmpty(nick)) {
            showName = username;
        } else {
            showName = String.format("%s (%s)", nick, username);
        }
    }

}