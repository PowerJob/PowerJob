package tech.powerjob.server.web.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.powerjob.server.persistence.QueryConvertUtils;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.request.QueryUserRequest;
import tech.powerjob.server.web.response.UserBaseVO;
import tech.powerjob.server.web.service.UserWebService;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.List;
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

            return Optional.of(UserConverter.do2BaseVo(userInfoDO, false));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<UserInfoDO> list(QueryUserRequest q) {

        Long userIdEq = q.getUserIdEq();
        String accountTypeEq = q.getAccountTypeEq();
        String nickLike = q.getNickLike();
        String phoneLike = q.getPhoneLike();


        Specification<UserInfoDO> specification = (root, query, cb) -> {

            List<Predicate> predicates = Lists.newArrayList();

            if (userIdEq != null) {
                predicates.add(cb.equal(root.get("id"), userIdEq));
            }

            if (StringUtils.isNotEmpty(accountTypeEq)) {
                predicates.add(cb.equal(root.get("accountType"), accountTypeEq));
            }

            if (StringUtils.isNotEmpty(nickLike)) {
                predicates.add(cb.like(root.get("nick"), QueryConvertUtils.convertLikeParams(nickLike)));
            }

            if (StringUtils.isNotEmpty(phoneLike)) {
                predicates.add(cb.like(root.get("phone"), QueryConvertUtils.convertLikeParams(phoneLike)));
            }

            if (predicates.isEmpty()) {
                return null;
            }
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };

        return userInfoRepository.findAll(specification);
    }
}
