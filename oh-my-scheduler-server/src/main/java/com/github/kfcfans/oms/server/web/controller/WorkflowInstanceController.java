package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.oms.server.service.workflow.WorkflowInstanceService;
import com.github.kfcfans.oms.server.web.request.QueryWorkflowInstanceRequest;
import com.github.kfcfans.oms.server.web.response.WorkflowInstanceInfoVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 工作流实例控制器
 *
 * @author tjq
 * @since 2020/5/31
 */
@RestController
@RequestMapping("/wfInstance")
public class WorkflowInstanceController {

    @Resource
    private WorkflowInstanceService workflowInstanceService;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    @GetMapping("/stop")
    public ResultDTO<Void> stopWfInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.stopWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<WorkflowInstanceInfoVO>> listWfInstance(@RequestBody QueryWorkflowInstanceRequest req) {
        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(req.getIndex(), req.getPageSize(), sort);

        Page<WorkflowInstanceInfoDO> ps;
        if (req.getWfInstanceId() == null && req.getWorkflowId() == null) {
            ps = workflowInstanceInfoRepository.findByAppId(req.getAppId(), pageable);
        }else if (req.getWfInstanceId() != null) {
            ps = workflowInstanceInfoRepository.findByAppIdAndWfInstanceId(req.getAppId(), req.getWfInstanceId(), pageable);
        }else {
            ps = workflowInstanceInfoRepository.findByAppIdAndWorkflowId(req.getAppId(), req.getWorkflowId(), pageable);
        }
        return ResultDTO.success(convertPage(ps));
    }

    private static PageResult<WorkflowInstanceInfoVO> convertPage(Page<WorkflowInstanceInfoDO> ps) {
        PageResult<WorkflowInstanceInfoVO> pr = new PageResult<>(ps);
        pr.setData(ps.getContent().stream().map(WorkflowInstanceInfoVO::from).collect(Collectors.toList()));
        return pr;
    }
}
