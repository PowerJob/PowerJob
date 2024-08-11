package tech.powerjob.server.web.service;

import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.web.request.ChangePasswordRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;

/**
 * PwjbUserWebService
 *
 * @author tjq
 * @since 2024/2/15
 */
public interface PwjbUserWebService {

    PwjbUserInfoDO save(ModifyUserInfoRequest request);

    void changePassword(ChangePasswordRequest changePasswordRequest);
}
