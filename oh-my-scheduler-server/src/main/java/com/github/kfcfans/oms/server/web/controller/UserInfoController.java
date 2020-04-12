package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.model.UserInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.UserInfoRepository;
import com.github.kfcfans.oms.server.web.request.ModifyUserInfoRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
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
    private UserInfoRepository userInfoRepository;

    @PostMapping("save")
    public ResultDTO<Void> save(ModifyUserInfoRequest request) {
        UserInfoDO userInfoDO = new UserInfoDO();
        BeanUtils.copyProperties(request, userInfoDO);
        userInfoDO.setGmtCreate(new Date());
        userInfoDO.setGmtModified(userInfoDO.getGmtCreate());
        userInfoRepository.saveAndFlush(userInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("list")
    public ResultDTO<List<UserItemVO>> list() {
        List<UserItemVO> result = userInfoRepository.findAll().stream().map(x -> new UserItemVO(x.getId(), x.getUsername())).collect(Collectors.toList());
        return ResultDTO.success(result);
    }


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class UserItemVO {
        private Long id;
        private String username;
    }
}
