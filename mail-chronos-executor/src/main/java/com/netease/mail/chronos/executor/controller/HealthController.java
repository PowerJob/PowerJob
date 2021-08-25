package com.netease.mail.chronos.executor.controller;

import com.netease.mail.chronos.executor.response.BaseResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Echo009
 * @since 2021/8/25
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @RequestMapping("/check")
    public BaseResponse<String> check(){
        return BaseResponse.success();
    }

}
