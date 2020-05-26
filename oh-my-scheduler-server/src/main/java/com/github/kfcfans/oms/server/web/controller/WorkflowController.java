package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.service.workflow.WorkflowService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 工作流控制器
 *
 * @author tjq
 * @since 2020/5/26
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @PostMapping("/save")
    public ResultDTO<Long> save(@RequestBody SaveWorkflowRequest req) throws Exception {
        return ResultDTO.success(workflowService.saveWorkflow(req));
    }

}
