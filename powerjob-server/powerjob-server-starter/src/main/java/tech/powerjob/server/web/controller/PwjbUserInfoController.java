package tech.powerjob.server.web.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.web.request.ChangePasswordRequest;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.service.PwjbUserWebService;

import javax.annotation.Resource;

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
    private PwjbUserWebService pwjbUserWebService;

    /**
     * 创建第三方登录体系（PowerJob） 的账户，不允许修改
     * @param request 请求（此处复用了主框架请求，便于用户一次性把所有参数都填入）
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResultDTO<Void> save(@RequestBody ModifyUserInfoRequest request) {
        pwjbUserWebService.save(request);
        return ResultDTO.success(null);
    }

    @PostMapping("/changePassword")
    public ResultDTO<Void> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {

        pwjbUserWebService.changePassword(changePasswordRequest);
        return ResultDTO.success(null);
    }
}
