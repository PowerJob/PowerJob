package com.github.kfcfans.powerjob.server.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.service.migrate.MigrateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Help users upgrade from a low version of powerjob-server to a high version of powerjob-server
 * v4 means that this interface was upgraded from version v3.x to v4.x, and so on
 *
 * @author tjq
 * @author Echo009
 * @since 2021/2/23
 */
@Slf4j
@RestController
@RequestMapping("/migrate")
public class MigrateController {


    @Resource
    private MigrateService migrateService;

    /**
     * 修复对应 APP 下的任务信息
     */
    @RequestMapping("/v4/job")
    public ResultDTO<JSONObject> fixJobInfoFromV3ToV4(@RequestParam Long appId) {
        return ResultDTO.success(migrateService.fixDeprecatedProcessType(appId));
    }

    /**
     * 修复对应 APP 下的工作流信息
     */
    @RequestMapping("/v4/workflow")
    public ResultDTO<JSONObject> fixWorkflowInfoFromV3ToV4(@RequestParam Long appId){
        return ResultDTO.success(migrateService.fixWorkflowInfoFromV3ToV4(appId));
    }


}
