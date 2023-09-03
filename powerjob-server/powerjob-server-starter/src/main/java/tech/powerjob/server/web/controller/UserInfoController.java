package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.core.service.UserService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.converter.UserConverter;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.response.UserBaseVO;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息控制层
 *
 * @author tjq
 * @since 2020/4/12
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Resource
    private UserService userService;
    @Resource
    private UserInfoRepository userInfoRepository;

    @PostMapping("save")
    public ResultDTO<Void> save(@RequestBody ModifyUserInfoRequest request) {
        UserInfoDO userInfoDO = new UserInfoDO();
        BeanUtils.copyProperties(request, userInfoDO);
        userService.save(userInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("list")
    public ResultDTO<List<UserBaseVO>> list(@RequestParam(required = false) String name) {

        List<UserInfoDO> result;
        if (StringUtils.isEmpty(name)) {
            result = userInfoRepository.findAll();
        }else {
            result = userInfoRepository.findByUsernameLike("%" + name + "%");
        }
        return ResultDTO.success(convert(result));
    }

    private static List<UserBaseVO> convert(List<UserInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(UserConverter::do2BaseVo).collect(Collectors.toList());
    }
}
