package tech.powerjob.server.web.service.impl;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.auth.common.AuthErrorCode;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.common.utils.DigestUtils;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.persistence.remote.repository.PwjbUserInfoRepository;
import tech.powerjob.server.web.request.ChangePasswordRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.service.PwjbUserWebService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 * PwjbUserWebService
 *
 * @author tjq
 * @since 2024/2/15
 */
@Service
public class PwjbUserWebServiceImplImpl implements PwjbUserWebService {

    @Resource
    private PwjbUserInfoRepository pwjbUserInfoRepository;

    @Override
    @SneakyThrows
    public PwjbUserInfoDO save(ModifyUserInfoRequest request) {
        String username = request.getUsername();
        CommonUtils.requireNonNull(username, "userName can't be null or empty!");
        Optional<PwjbUserInfoDO> oldUserOpt = pwjbUserInfoRepository.findByUsername(username);
        if (oldUserOpt.isPresent()) {
            throw new IllegalArgumentException("username already exist, please change one!");
        }

        PwjbUserInfoDO pwjbUserInfoDO = new PwjbUserInfoDO();

        pwjbUserInfoDO.setUsername(username);
        pwjbUserInfoDO.setGmtCreate(new Date());
        pwjbUserInfoDO.setGmtModified(new Date());

        // 二次加密密码
        final String password = request.getPassword();
        if (StringUtils.isNotEmpty(password)) {
            pwjbUserInfoDO.setPassword(DigestUtils.rePassword(password, pwjbUserInfoDO.getUsername()));
        }

        // 其他参数存入 extra，在回调创建真正的内部 USER 时回填
        ModifyUserInfoRequest cpRequest = JsonUtils.parseObject(JsonUtils.toJSONString(request), ModifyUserInfoRequest.class);
        cpRequest.setPassword(null);
        cpRequest.setUsername(null);
        cpRequest.setNick(null);
        pwjbUserInfoDO.setExtra(JsonUtils.toJSONString(cpRequest));

        return pwjbUserInfoRepository.save(pwjbUserInfoDO);
    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest) {
        if (!StringUtils.equals(changePasswordRequest.getNewPassword(), changePasswordRequest.getNewPassword2())) {
            throw new IllegalArgumentException("Inconsistent passwords");
        }

        String username = changePasswordRequest.getUsername();
        Optional<PwjbUserInfoDO> userOpt = pwjbUserInfoRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("can't find user by username: " + username);
        }

        PwjbUserInfoDO dbUser = userOpt.get();
        String oldPasswordInDb = dbUser.getPassword();
        String oldPasswordInReq = DigestUtils.rePassword(changePasswordRequest.getOldPassword(), dbUser.getUsername());
        if (!StringUtils.equals(oldPasswordInDb, oldPasswordInReq)) {
            throw new PowerJobAuthException(AuthErrorCode.INCORRECT_PASSWORD);
        }

        dbUser.setPassword(DigestUtils.rePassword(changePasswordRequest.getNewPassword(), dbUser.getUsername()));
        dbUser.setGmtModified(new Date());
        pwjbUserInfoRepository.saveAndFlush(dbUser);
    }
}
