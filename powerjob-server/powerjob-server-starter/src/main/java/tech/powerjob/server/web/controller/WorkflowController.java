package tech.powerjob.server.web.controller;

import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.WorkflowInfoRepository;
import tech.powerjob.server.core.workflow.WorkflowService;
import tech.powerjob.server.web.request.QueryWorkflowInfoRequest;
import tech.powerjob.server.web.response.WorkflowInfoVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.List;
import java.util.function.LongToDoubleFunction;
import java.util.stream.Collectors;

/**
 * 工作流控制器
 *
 * @author tjq
 * @author zenggonggu
 * @since 2020/5/26
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;

    @PostMapping("/save")
    public ResultDTO<Long> save(@RequestBody SaveWorkflowRequest req) throws ParseException {
        return ResultDTO.success(workflowService.saveWorkflow(req));
    }

    @PostMapping("/copy")
    public ResultDTO<Long> copy(Long workflowId, Long appId) {
        return ResultDTO.success(workflowService.copyWorkflow(workflowId,appId));
    }

    @GetMapping("/disable")
    public ResultDTO<Void> disableWorkflow(Long workflowId, Long appId) {
        workflowService.disableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/enable")
    public ResultDTO<Void> enableWorkflow(Long workflowId, Long appId) {
        workflowService.enableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteWorkflow(Long workflowId, Long appId) {
        workflowService.deleteWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<WorkflowInfoVO>> list(@RequestBody QueryWorkflowInfoRequest req) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtCreate");
        PageRequest pageRequest = PageRequest.of(req.getIndex(), req.getPageSize(), sort);
        Page<WorkflowInfoDO> wfPage;

        // 排除已删除数据
        int nStatus = SwitchableStatus.DELETED.getV();
        // 无查询条件，查询全部
        if (req.getWorkflowId() == null && StringUtils.isEmpty(req.getKeyword())) {
            wfPage = workflowInfoRepository.findByAppIdAndStatusNot(req.getAppId(), nStatus, pageRequest);
        }else if (req.getWorkflowId() != null) {
            wfPage = workflowInfoRepository.findByIdAndStatusNot(req.getWorkflowId(), nStatus, pageRequest);
        }else {
            String condition = "%" + req.getKeyword() + "%";
            wfPage = workflowInfoRepository.findByAppIdAndStatusNotAndWfNameLike(req.getAppId(), nStatus, condition, pageRequest);
        }
        return ResultDTO.success(convertPage(wfPage));
    }

    @GetMapping("/run")
    public ResultDTO<Long> runWorkflow(Long workflowId, Long appId,
                                       @RequestParam(required = false,defaultValue = "0") Long delay,
                                       @RequestParam(required = false) String initParams
                                       ) {
        return ResultDTO.success(workflowService.runWorkflow(workflowId, appId, initParams, delay));
    }

    @GetMapping("/fetch")
    public ResultDTO<WorkflowInfoVO> fetchWorkflow(Long workflowId, Long appId) {
        WorkflowInfoDO workflowInfoDO = workflowService.fetchWorkflow(workflowId, appId);
        return ResultDTO.success(WorkflowInfoVO.from(workflowInfoDO));
    }

    @PostMapping("/saveNode")
    public ResultDTO<List<WorkflowNodeInfoDO>> addWorkflowNode(@RequestBody List<SaveWorkflowNodeRequest> request) {
        return ResultDTO.success(workflowService.saveWorkflowNode(request));
    }


    private static PageResult<WorkflowInfoVO> convertPage(Page<WorkflowInfoDO> originPage) {

        PageResult<WorkflowInfoVO> newPage = new PageResult<>(originPage);
        newPage.setData(originPage.getContent().stream().map(WorkflowInfoVO::from).collect(Collectors.toList()));
        return newPage;
    }

}
