package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.UserInfoRepository;
import com.github.kfcfans.oms.server.service.UserService;
import com.github.kfcfans.oms.server.web.request.ModifyUserInfoRequest;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
        userService.save(request);
        return ResultDTO.success(null);
    }

    @GetMapping("list")
    public ResultDTO<List<UserItemVO>> list(@RequestParam(required = false) String name) {

        List<UserInfoDO> result;
        if (StringUtils.isEmpty(name)) {
            result = userInfoRepository.findAll();
        }else {
            result = userInfoRepository.findByUsernameLike("%" + name + "%");
        }
        return ResultDTO.success(convert(result));
    }

    private static List<UserItemVO> convert(List<UserInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(x -> new UserItemVO(x.getId(), x.getUsername())).collect(Collectors.toList());
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class UserItemVO {
        private Long id;
        private String username;
    }
}
