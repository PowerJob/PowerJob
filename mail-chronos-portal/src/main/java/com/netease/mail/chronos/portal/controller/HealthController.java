package com.netease.mail.chronos.portal.controller;

import com.netease.mail.chronos.base.response.BaseResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Echo009
 * @since 2021/9/24
 */
@RestController
@RequestMapping("/health")
public class HealthController {


    @RequestMapping("/check")
    public BaseResponse<String> check(){
        return BaseResponse.success("~");
    }

}
