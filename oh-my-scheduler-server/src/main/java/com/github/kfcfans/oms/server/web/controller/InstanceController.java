package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.oms.server.service.instance.InstanceDetail;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * 任务实例 Controller
 *
 * @author tjq
 * @since 2020/4/9
 */
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Resource
    private InstanceService instanceService;

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/status")
    public ResultDTO<InstanceDetail> getRunningStatus(Long instanceId) {
        return ResultDTO.success(instanceService.getInstanceDetail(instanceId));
    }
}
