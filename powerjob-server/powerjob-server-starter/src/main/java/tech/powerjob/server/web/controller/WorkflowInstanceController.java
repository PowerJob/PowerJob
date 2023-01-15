package tech.powerjob.server.web.controller;

import tech.powerjob.common.enums.WorkflowInstanceStatus;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInstanceInfoRepository;
import tech.powerjob.server.core.service.CacheService;
import tech.powerjob.server.core.workflow.WorkflowInstanceService;
import tech.powerjob.server.web.request.QueryWorkflowInstanceRequest;
import tech.powerjob.server.web.response.WorkflowInstanceInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.apache.commons.lang3.StringUtils;
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
        workflowInstanceService.stopWorkflowInstanceEntrance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @RequestMapping("/retry")
    public ResultDTO<Void> retryWfInstance(Long wfInstanceId, Long appId) {
        workflowInstanceService.retryWorkflowInstance(wfInstanceId, appId);
        return ResultDTO.success(null);
    }

    @RequestMapping("/markNodeAsSuccess")
    public ResultDTO<Void> markNodeAsSuccess(Long wfInstanceId, Long appId, Long nodeId) {
        workflowInstanceService.markNodeAsSuccess(appId, wfInstanceId, nodeId);
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
