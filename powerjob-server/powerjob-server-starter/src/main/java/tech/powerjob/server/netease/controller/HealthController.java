package tech.powerjob.server.netease.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;

import java.util.Collections;
import java.util.Map;

/**
 * @author Echo009
 * @since 2021/9/14
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @RequestMapping("/check")
    public ResultDTO<Map<String,String>> check(){
        return ResultDTO.success(Collections.emptyMap());
    }

}
