package tech.powerjob.server.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.auth.common.AuthErrorCode;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.common.utils.DigestUtils;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.persistence.remote.repository.PwjbUserInfoRepository;
import tech.powerjob.server.web.request.ChangePasswordRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
 * PowerJob 自带的登录体系
 * （同样视为第三方服务，与主框架没有任何关系）
 *
 * @author tjq
 * @since 2024/2/13
 */
@RestController
@RequestMapping("/pwjbUser")
public class PwjbUserInfoController {

    @Resource
    private PwjbUserInfoRepository pwjbUserInfoRepository;

    /**
     * 创建第三方登录体系（PowerJob） 的账户，不允许修改
     * @param request 请求（此处复用了主框架请求，便于用户一次性把所有参数都填入）
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResultDTO<Void> save(@RequestBody ModifyUserInfoRequest request) {

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
        request.setPassword(null);
        pwjbUserInfoDO.setExtra(JsonUtils.toJSONString(request));

        pwjbUserInfoRepository.save(pwjbUserInfoDO);
        return ResultDTO.success(null);
    }

    @PostMapping("/changePassword")
    public ResultDTO<Void> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {

        if (StringUtils.equals(changePasswordRequest.getNewPassword(), changePasswordRequest.getNewPassword2())) {
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

        return ResultDTO.success(null);
    }
}
