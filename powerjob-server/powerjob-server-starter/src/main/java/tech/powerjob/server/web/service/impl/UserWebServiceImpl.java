package tech.powerjob.server.web.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.service.UserWebService;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * UserWebService
 *
 * @author tjq
 * @since 2024/2/17
 */
@Service
public class UserWebServiceImpl implements UserWebService {

    /**
     * 展示用的 user 查询缓存，对延迟不敏感
     */
    private final Cache<Long, UserInfoDO> userCache4Show = CacheBuilder.newBuilder()
            .softValues()
            .maximumSize(256)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    @Resource
    private UserInfoRepository userInfoRepository;

    @Override
    public Optional<UserBaseVO> fetchBaseUserInfo(Long userId) {

        if (userId == null) {
            return Optional.empty();
        }

        try {
            UserInfoDO userInfoDO = userCache4Show.get(userId, () -> {
                Optional<UserInfoDO> userInfoOpt = userInfoRepository.findById(userId);
                if (userInfoOpt.isPresent()) {
                    return userInfoOpt.get();
                }
                throw new IllegalArgumentException("can't find user by userId: " + userId);
            });

            return Optional.of(UserConverter.do2BaseVo(userInfoDO));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
