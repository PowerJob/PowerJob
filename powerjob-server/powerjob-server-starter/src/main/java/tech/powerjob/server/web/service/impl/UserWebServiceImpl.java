package tech.powerjob.server.web.service.impl;

import org.springframework.stereotype.Service;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.service.UserWebService;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * UserWebService
 *
 * @author tjq
 * @since 2024/2/17
 */
@Service
public class UserWebServiceImpl implements UserWebService {

    @Resource
    private UserInfoRepository userInfoRepository;

    @Override
    public Optional<UserBaseVO> fetchBaseUserInfo(Long userId) {

        if (userId == null) {
            return Optional.empty();
        }

        Optional<UserInfoDO> userInfoOpt = userInfoRepository.findById(userId);
        return userInfoOpt.map(UserConverter::do2BaseVo);
    }
}
