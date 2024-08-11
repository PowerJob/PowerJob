package tech.powerjob.server.web.service;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.web.request.QueryUserRequest;
import tech.powerjob.server.web.response.UserBaseVO;

import java.util.List;
import java.util.Optional;

/**
 * 用户 WEB 服务
 *
 * @author tjq
 * @since 2024/2/17
 */
public interface UserWebService {

    Optional<UserBaseVO> fetchBaseUserInfo(Long userId);

    List<UserInfoDO> list(QueryUserRequest queryUserRequest);
}
