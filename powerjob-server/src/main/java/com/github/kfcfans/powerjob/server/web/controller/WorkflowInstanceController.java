package com.github.kfcfans.powerjob.server.web.controller;

import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.persistence.PageResult;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.CacheService;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowInstanceService;
import com.github.kfcfans.powerjob.server.web.request.QueryWorkflowInstanceRequest;
import com.github.kfcfans.powerjob.server.web.response.WorkflowInstanceInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
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
    private CacheService cacheService;
    @Resource
    private WorkflowInstanceService workflowInstanceService;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    @GetMapping("/stop")
    public ResultDTO<Void> stopWfInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.stopWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @RequestMapping("/retry")
    public ResultDTO<Void> retryWfInstance(Long wfInstanceId, Long appId){
        workflowInstanceService.retryWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/info")
    public ResultDTO<WorkflowInstanceInfoVO> getInfo(Long wfInstanceId, Long appId) {
        WorkflowInstanceInfoDO wfInstanceDO = workflowInstanceService.fetchWfInstance(wfInstanceId, appId);
        return ResultDTO.success(WorkflowInstanceInfoVO.from(wfInstanceDO, cacheService.getWorkflowName(wfInstanceDO.getWorkflowId())));
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<WorkflowInstanceInfoVO>> listWfInstance(@RequestBody QueryWorkflowInstanceRequest req) {
        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(req.getIndex(), req.getPageSize(), sort);

        WorkflowInstanceInfoDO queryEntity = new WorkflowInstanceInfoDO();
        BeanUtils.copyProperties(req, queryEntity);

        if (!StringUtils.isEmpty(req.getStatus())) {
            queryEntity.setStatus(WorkflowInstanceStatus.valueOf(req.getStatus()).getV());
        }

        Page<WorkflowInstanceInfoDO> ps = workflowInstanceInfoRepository.findAll(Example.of(queryEntity), pageable);

        return ResultDTO.success(convertPage(ps));
    }

    private PageResult<WorkflowInstanceInfoVO> convertPage(Page<WorkflowInstanceInfoDO> ps) {
        PageResult<WorkflowInstanceInfoVO> pr = new PageResult<>(ps);
        pr.setData(ps.getContent().stream().map(x -> WorkflowInstanceInfoVO.from(x, cacheService.getWorkflowName(x.getWorkflowId()))).collect(Collectors.toList()));
        return pr;
    }
}
