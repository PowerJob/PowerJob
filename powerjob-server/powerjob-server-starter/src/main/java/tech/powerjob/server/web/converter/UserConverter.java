package tech.powerjob.server.web.converter;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.web.response.UserBaseVO;

/**
 * UserConverter
 *
 * @author tjq
 * @since 2023/9/4
 */
public class UserConverter {

    public static UserBaseVO do2BaseVo(UserInfoDO x) {

        UserBaseVO userBaseVO = new UserBaseVO();

        userBaseVO.setId(x.getId());
        userBaseVO.setUsername(x.getUsername());
        userBaseVO.setNick(x.getNick());

        userBaseVO.genShowName();
        return userBaseVO;
    }

}
