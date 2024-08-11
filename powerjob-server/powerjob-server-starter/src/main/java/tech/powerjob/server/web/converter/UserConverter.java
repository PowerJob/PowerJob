package tech.powerjob.server.web.converter;

import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.web.response.UserBaseVO;

import java.util.Optional;

/**
 * UserConverter
 *
 * @author tjq
 * @since 2023/9/4
 */
public class UserConverter {

    public static UserBaseVO do2BaseVo(UserInfoDO x, boolean includeSensitiveInfo) {

        UserBaseVO userBaseVO = new UserBaseVO();

        userBaseVO.setId(x.getId());
        userBaseVO.setAccountType(x.getAccountType());
        userBaseVO.setUsername(x.getUsername());
        userBaseVO.setNick(x.getNick());
        userBaseVO.setStatus(Optional.ofNullable(x.getStatus()).orElse(SwitchableStatus.ENABLE.getV()));
        userBaseVO.setEnable(userBaseVO.getStatus() == SwitchableStatus.ENABLE.getV());

        if (includeSensitiveInfo) {
            userBaseVO.setPhone(x.getPhone());
            userBaseVO.setEmail(x.getEmail());
        }

        userBaseVO.genShowName();
        return userBaseVO;
    }

}
